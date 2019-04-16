package bio.overture.maestro.app.infra.adapter.outbound.indexing.elasticsearch;

import bio.overture.maestro.app.infra.config.RootConfiguration;
import bio.overture.maestro.app.infra.config.properties.ApplicationProperties;
import bio.overture.maestro.domain.api.exception.FailureData;
import bio.overture.maestro.domain.api.message.IndexResult;
import bio.overture.maestro.domain.entities.indexing.FileCentricDocument;
import bio.overture.maestro.domain.port.outbound.indexing.BatchIndexFilesCommand;
import bio.overture.maestro.domain.port.outbound.indexing.FileCentricIndexAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.vavr.control.Try;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.inject.Inject;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static bio.overture.maestro.domain.utility.CollectionsUtil.partitionList;
import static bio.overture.maestro.domain.utility.StringUtilities.inputStreamToString;
import static java.util.Collections.singletonMap;

@Slf4j
class FileCentricElasticSearchAdapter implements FileCentricIndexAdapter {

    private static final String ANALYSIS_ID = "analysisId";
    private static final int FALL_BACK_WAIT_DURATION = 100;
    private static final int FALLBACK_MAX_RETRY_ATTEMPTS = 0;
    private final CustomElasticSearchRestAdapter customElasticSearchRestAdapter;
    private final Resource indexSettings;
    private final Resource fileCentricMapping;
    private final ElasticsearchRestTemplate template;
    private final String alias;
    private final int documentsPerBulkRequest;
    private final int maxRetriesAttempts;
    private final long retriesWaitDuration;
    /**
     * we define a writer to save properties inferring at runtime for each object.
     */
    private final ObjectWriter fileCentricJSONWriter;

    @Inject
    public FileCentricElasticSearchAdapter(CustomElasticSearchRestAdapter customElasticSearchRestAdapter,
                                           ElasticsearchRestTemplate template,
                                           @Qualifier(RootConfiguration.ELASTIC_SEARCH_DOCUMENT_JSON_MAPPER) ObjectMapper objectMapper,
                                           ApplicationProperties properties) {

        this.customElasticSearchRestAdapter = customElasticSearchRestAdapter;
        this.fileCentricMapping = properties.fileCentricMapping();
        this.template = template;
        this.alias = properties.fileCentricAlias();
        this.documentsPerBulkRequest = properties.maxDocsPerBulkRequest();
        this.indexSettings = properties.indexSettings();
        this.fileCentricJSONWriter = objectMapper.writerFor(FileCentricDocument.class);
        this.retriesWaitDuration = properties.elasticSearchRetryWaitDurationMillis() > 0 ?
            properties.elasticSearchRetryWaitDurationMillis() : FALL_BACK_WAIT_DURATION;
        this.maxRetriesAttempts = properties.elasticSearchRetryMaxAttempts() >= 0 ?
            properties.elasticSearchRetryMaxAttempts() : FALLBACK_MAX_RETRY_ATTEMPTS;
    }

    @Override
    public Mono<IndexResult> batchIndex(@NonNull BatchIndexFilesCommand batchIndexFilesCommand) {
        log.debug("in batchIndex, args: {} ", batchIndexFilesCommand.getFiles().size());
        return  Mono.fromSupplier(() -> this.bulkIndexFiles(batchIndexFilesCommand.getFiles()))
            .subscribeOn(Schedulers.elastic());
    }

    @Override
    public Mono<IndexResult> batchUpsertFileRepositories(@NonNull BatchIndexFilesCommand batchIndexFilesCommand) {
        log.debug("in batchIbatchUpsertFileRepositoriesndex, args: {} ", batchIndexFilesCommand.getFiles().size());
        return  Mono.fromSupplier(() -> this.bulkUpsertFileRepositories(batchIndexFilesCommand.getFiles()))
            .subscribeOn(Schedulers.elastic());
    }

    @Retryable(
        maxAttempts = 5,
        backoff = @Backoff(value = 1000, multiplier=1.5)
    )
    void initialize() {
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
    void recover(Throwable t) {
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
        partitionList(filesList, this.documentsPerBulkRequest)
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
        val failures = partitionList(filesList, size)
            .entrySet()
            .stream()
            .map((entry)-> tryBulkUpsertRequestForPart(filesList, entry))
            .flatMap(Set::stream)
            .collect(Collectors.toUnmodifiableSet());

        val fails = FailureData.builder()
            .failingIds(Map.of(ANALYSIS_ID, failures))
            .build();

        return IndexResult.builder()
            .failureData(fails)
            .successful(failures.isEmpty())
            .build();
    }

    @NotNull
    private Set<String> tryBulkUpsertRequestForPart(List<FileCentricDocument> filesList,
                                                    Map.Entry<Integer, List<FileCentricDocument>> entry) {
        val partNum = entry.getKey();
        val listPart = entry.getValue();
        val listParthHash = Objects.hashCode(listPart);
        val fileListHash =  Objects.hashCode(filesList);
        val retryConfig = RetryConfig.custom()
            .maxAttempts(this.maxRetriesAttempts)
            .retryExceptions(IOException.class)
            .waitDuration(Duration.ofMillis(this.retriesWaitDuration))
            .build();
        val retry = Retry.of("bulkUpsertFileRepositoriesRetry", retryConfig);
        val decorated = Retry.decorateCheckedSupplier(retry, () -> {
            log.trace("bulkUpsertFileRepositories, sending part#: {}, hash: {} for filesList hash: {} ", partNum,
                listParthHash, fileListHash);
            doRequestForPart(listPart);
            return Set.<String>of();
        });
        val result = Try.of(decorated)
            .recover((t) -> {
                log.error("failed sending request for: part#: {}, hash: {} for filesList hash: {} to elastic search," +
                    " gathering failed Ids.", partNum, listParthHash, fileListHash, t);
                return listPart.stream()
                    .map(fileCentricDocument -> fileCentricDocument.getAnalysis().getId())
                    .collect(Collectors.toUnmodifiableSet());
            });
        return result.get();
    }

    private void doRequestForPart(List<FileCentricDocument> listPart) throws IOException {
        this.customElasticSearchRestAdapter.bulkUpdateRequest(
            listPart.stream()
                .map(this::mapFileToUpsertRepositoryQuery)
                .collect(Collectors.toList())
        );
    }

    @SneakyThrows
    private String loadMappingMap(String typeName) {
        log.trace("in loadMappingMap: {}", typeName);
        return inputStreamToString(fileCentricMapping.getInputStream());
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
        val mapper = new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
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
