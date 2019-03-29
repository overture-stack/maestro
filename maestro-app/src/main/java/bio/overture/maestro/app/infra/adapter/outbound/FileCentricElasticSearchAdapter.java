package bio.overture.maestro.app.infra.adapter.outbound;

import bio.overture.maestro.app.infra.config.ApplicationProperties;
import bio.overture.maestro.app.infra.config.RootConfiguration;
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
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static bio.overture.maestro.domain.utility.CollectionsUtil.partitionList;
import static bio.overture.maestro.domain.utility.StringUtilities.inputStreamToString;
import static java.util.Collections.singletonMap;

@Slf4j
public class FileCentricElasticSearchAdapter implements FileCentricIndexAdapter {

    private final CustomElasticSearchRestAdapter customElasticSearchRestAdapter;
    private final Resource indexSettings;
    private final ResourceLoader resourceLoader;
    private ElasticsearchRestTemplate template;
    private String alias;
    private int documentsPerBulkRequest;
    /**
     * we define a writer to save properties inferring at runtime for each object.
     */
    private ObjectWriter fileCentricJSONWriter;

    @Inject
    public FileCentricElasticSearchAdapter(CustomElasticSearchRestAdapter customElasticSearchRestAdapter,
                                           ElasticsearchRestTemplate template,
                                           ResourceLoader resourceLoader,
                                           @Qualifier(RootConfiguration.ELASTIC_SEARCH_DOCUMENT_JSON_MAPPER) ObjectMapper objectMapper,
                                           ApplicationProperties properties) {
        this.customElasticSearchRestAdapter = customElasticSearchRestAdapter;
        this.template = template;
        this.alias = properties.getFileCentricAlias();
        this.documentsPerBulkRequest = properties.getDocsPerBulkMax();
        this.indexSettings = properties.getIndexSettings();
        this.resourceLoader = resourceLoader;
        this.fileCentricJSONWriter = objectMapper.writerFor(FileCentricDocument.class);
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
    public Mono<IndexResult> batchUpsertFileRepositories(@NonNull BatchIndexFilesCommand batchIndexFilesCommand) {
        log.debug("in batchIbatchUpsertFileRepositoriesndex, args: {} ", batchIndexFilesCommand.getFiles().size());
        return  Mono.fromSupplier(() -> this.bulkUpsertFileRepositories(batchIndexFilesCommand.getFiles()))
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
        val size = this.documentsPerBulkRequest;
        partitionList(filesList, size)
            .forEach((partNum, listPart)-> {
                log.trace("bulkIndexFiles, sending part#: {}, hash: {} for filesList hash: {} ", partNum,
                    Objects.hashCode(listPart), Objects.hashCode(filesList));
                template.bulkIndex(listPart.stream()
                    .map(this::mapFileToIndexQuery)
                    .collect(Collectors.toList())
                );
            });
        return IndexResult.builder().successful(true).build();
    }

    @SneakyThrows
    private IndexResult bulkUpsertFileRepositories(List<FileCentricDocument> filesList) {
        log.trace("in bulkUpsertFileRepositories, filesList count : {} ", filesList.size());
        val size = this.documentsPerBulkRequest;
        partitionList(filesList, size)
            .forEach((partNum, listPart)-> {
                log.trace("bulkUpsertFileRepositories, sending part#: {}, hash: {} for filesList hash: {} ", partNum,
                    Objects.hashCode(listPart), Objects.hashCode(filesList));
                this.customElasticSearchRestAdapter.bulkUpdateRequest(listPart.stream()
                    .map(this::mapFileToUpsertRepositoryQuery)
                    .collect(Collectors.toList())
                );
            });
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
    private UpdateRequest mapFileToUpsertRepositoryQuery(FileCentricDocument fileCentricDocument){
        ObjectMapper mapper = new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        Map<String, Object> parameters = singletonMap("repository",
            mapper.convertValue(fileCentricDocument.getRepositories().get(0), Map.class));

        Script inline = new Script(ScriptType.INLINE,
            "painless",
            "if (!ctx._source.repositories.contains(params.repository)) { ctx._source.repositories.add(params.repository) }",
            parameters
        );

        return new UpdateRequest()
            .id(fileCentricDocument.getObjectId())
            .index(this.alias)
            .type(this.alias)
            .script(inline)
            .upsert(new IndexRequest()
                .index(this.alias)
                .type(this.alias)
                .id(fileCentricDocument.getObjectId())
                .source(fileCentricJSONWriter.writeValueAsString(fileCentricDocument), XContentType.JSON)
            );
    }

}
