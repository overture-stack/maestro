package bio.overture.maestro.app.infra.adapter.outbound;

import bio.overture.maestro.app.infra.config.ApplicationProperties;
import bio.overture.maestro.domain.api.exception.UpstreamServiceException;
import bio.overture.maestro.domain.api.message.IndexResult;
import bio.overture.maestro.domain.entities.indexer.FileCentricDocument;
import bio.overture.maestro.domain.port.outbound.FileCentricIndexAdapter;
import bio.overture.maestro.domain.port.outbound.message.BatchIndexFilesCommand;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.data.elasticsearch.core.query.UpdateQueryBuilder;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static bio.overture.maestro.domain.utility.StringUtilities.inputStreamToString;
import static java.util.Collections.singletonMap;

@Slf4j
public class FileCentricElasticSearchAdapter implements FileCentricIndexAdapter {

    private final ElasticsearchRestTemplate template;
    private final Resource indexSettings;
    private final ResourceLoader resourceLoader;
    private String alias;
    private ObjectMapper objectMapper;
    /**
     * we define a writer to save properties inferring at runtime for each object.
     */
    private ObjectWriter fileCentricJSONWriter;

    @Inject
    public FileCentricElasticSearchAdapter(ElasticsearchRestTemplate template,
                                           ResourceLoader resourceLoader,
                                           ApplicationProperties properties) {
        this.template = template;
        this.alias = properties.getFileCentricAlias();
        this.indexSettings = properties.getIndexSettings();
        this.resourceLoader = resourceLoader;
        objectMapper = new ObjectMapper();
        // this to adhere to elastic search best practice of snake case field names.
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        fileCentricJSONWriter = objectMapper.writerFor(FileCentricDocument.class);
    }

    @Override
    public Mono<IndexResult> batchIndex(@NonNull BatchIndexFilesCommand batchIndexFilesCommand) {
        log.debug("in batchIndex, args: {} ", batchIndexFilesCommand.getFiles().size());
        return  Mono.fromSupplier(() -> this.bulkIndexFiles(batchIndexFilesCommand.getFiles()))
            .onErrorMap((e) -> e instanceof ElasticsearchException,
                (e) -> new UpstreamServiceException("batch Index failed", e))
            .subscribeOn(Schedulers.elastic());
    }

    @Override
    public Mono<IndexResult> batchUpdate(@NonNull BatchIndexFilesCommand batchIndexFilesCommand) {
        log.debug("in batchIndex, args: {} ", batchIndexFilesCommand.getFiles().size());
        return  Mono.fromSupplier(() -> this.bulkUpdateFiles(batchIndexFilesCommand.getFiles()))
            .onErrorMap((e) -> e instanceof ElasticsearchException,
                (e) -> new UpstreamServiceException("batch Index failed", e))
            .subscribeOn(Schedulers.elastic());
    }

    @Retryable(
        maxAttempts = 5,
        backoff = @Backoff(value = 1000, multiplier=1.5)
    )
    public void initialize() {
        try {
            val indexExists = this.template.indexExists(this.alias);
            val mappingExists = this.template.typeExists(this.alias, this.alias);

            log.info("indexExists result: {} ", indexExists);
            if (!indexExists) {
                this.createIndex();
                log.info("index {} have been created", this.alias, this.alias);
            }
            if (!mappingExists) {
                template.putMapping(this.alias, this.alias, loadMappingMap(this.alias));
                log.info("index {} with mapping {} have been created", this.alias, this.alias);
            }
        } catch (Exception e) {
            // we log here to document the failure if any each attempt.
            log.error("error while initializing ", e);
            //rethrow so a retry happens
            throw e;
        }
    }

    @Recover
    public void recover(Throwable t) {
        log.error("couldn't initialize the index", t);
    }

    /* *******************
     *  Private methods
     *********************/
    @SneakyThrows
    private void createIndex() {
        val indexSettings = inputStreamToString(this.indexSettings.getInputStream());
        this.template.createIndex(this.alias, indexSettings);
    }

    @SneakyThrows
    private IndexResult bulkIndexFiles(List<FileCentricDocument> filesList) {
        log.trace("in bulkIndexFiles, filesList count : {} ", filesList.size());
        this.template.bulkIndex(filesList.stream()
            .map(this::mapFileToIndexQuery)
            .collect(Collectors.toList())
        );
        return IndexResult.builder().successful(true).build();
    }

    @SneakyThrows
    private IndexResult bulkUpdateFiles(List<FileCentricDocument> filesList) {
        log.trace("in bulkIndexFiles, filesList count : {} ", filesList.size());
        this.template.bulkUpdate(filesList.stream()
            .map(this::mapFileToUpdateQuery)
            .collect(Collectors.toList())
        );
        return IndexResult.builder().successful(true).build();
    }

    @SneakyThrows
    private String loadMappingMap(String typeName) {
        log.trace("in loadMappingMap: {}", typeName);
        val mapping = this.resourceLoader.getResource("classpath:" + typeName + ".mapping.json");
        return inputStreamToString(mapping.getInputStream());
    }

    @SneakyThrows
    private IndexQuery mapFileToIndexQuery(FileCentricDocument fileCentricDocument){
        return new IndexQueryBuilder()
            .withId(fileCentricDocument.getObjectId())
            .withIndexName(this.alias)
            .withType(this.alias)
            .withSource(fileCentricJSONWriter.writeValueAsString(fileCentricDocument))
            .build();
    }

    @SneakyThrows
    private UpdateQuery mapFileToUpdateQuery(FileCentricDocument fileCentricDocument){
        Map<String, Object> parameters = singletonMap("repository", fileCentricDocument.getRepositories().get(0));
        Script inline = new Script(ScriptType.INLINE,
            "painless",
            "if (!ctx._source.repositories.contains(params.repository)) { ctx._source.repositories.add(params.repository)}",
            parameters
        );

        return new UpdateQueryBuilder()
            .withId(fileCentricDocument.getObjectId())
            .withIndexName(this.alias)
            .withType(this.alias)
            .withUpdateRequest(new UpdateRequest()
                .script(inline)
                .upsert(fileCentricDocument)
            )
            .build();
    }

}
