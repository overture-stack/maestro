package bio.overture.maestro.app.infra.adapter.outbound.indexing.elasticsearch;

import static bio.overture.maestro.app.infra.adapter.outbound.indexing.elasticsearch.SearchAdapterHelper.*;
import static bio.overture.maestro.domain.utility.StringUtilities.inputStreamToString;
import static java.lang.String.format;
import static java.util.Collections.unmodifiableMap;

import bio.overture.maestro.app.infra.config.RootConfiguration;
import bio.overture.maestro.app.infra.config.properties.ApplicationProperties;
import bio.overture.maestro.domain.api.message.IndexResult;
import bio.overture.maestro.domain.entities.indexing.analysis.AnalysisCentricDocument;
import bio.overture.maestro.domain.port.outbound.indexing.AnalysisCentricIndexAdapter;
import bio.overture.maestro.domain.port.outbound.indexing.BatchIndexAnalysisCommand;
import bio.overture.maestro.domain.utility.Parallel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
public class AnalysisCentricElasticSearchAdapter implements AnalysisCentricIndexAdapter {

  private RestHighLevelClient elasticsearchRestClient;

  private final ObjectWriter analysisCentricJSONWriter;

  private SnakeCaseJacksonSearchResultMapper searchResultMapper;

  private final Resource analysisCentricIndex;

  private final int documentsPerBulkRequest;

  private final int maxRetriesAttempts;

  private final String alias;
  private final String indexName;
  private final boolean enabled;
  private static final int FALLBACK_MAX_RETRY_ATTEMPTS = 0;
  private static final int FALL_BACK_WAIT_DURATION = 100;
  private final long retriesWaitDuration;
  private static final int MAX_PAGESIZE = 2000;

  @Inject
  public AnalysisCentricElasticSearchAdapter(
      RestHighLevelClient elasticsearchRestClient,
      @Qualifier(RootConfiguration.ELASTIC_SEARCH_DOCUMENT_JSON_MAPPER) ObjectMapper objectMapper,
      SnakeCaseJacksonSearchResultMapper searchResultMapper,
      ApplicationProperties properties) {
    this.elasticsearchRestClient = elasticsearchRestClient;
    this.analysisCentricJSONWriter = objectMapper.writerFor(AnalysisCentricDocument.class);
    this.searchResultMapper = searchResultMapper;
    this.analysisCentricIndex = properties.analysisCentricIndex();
    this.alias = properties.analysisCentricAlias();
    this.indexName = properties.analysisCentricIndexName();
    this.enabled = properties.isAnalysisCentricIndexEnabled();
    this.documentsPerBulkRequest = properties.maxDocsPerBulkRequest();
    this.maxRetriesAttempts =
        properties.elasticSearchRetryMaxAttempts() >= 0
            ? properties.elasticSearchRetryMaxAttempts()
            : FALLBACK_MAX_RETRY_ATTEMPTS;
    this.retriesWaitDuration =
        properties.elasticSearchRetryWaitDurationMillis() > 0
            ? properties.elasticSearchRetryWaitDurationMillis()
            : FALL_BACK_WAIT_DURATION;
  }

  @Override
  public Mono<IndexResult> batchUpsertAnalysisRepositories(
      @NonNull BatchIndexAnalysisCommand batchIndexAnalysisCommand) {
    return SearchAdapterHelper.batchUpsertDocuments(
        batchIndexAnalysisCommand.getAnalyses(),
        documentsPerBulkRequest,
        maxRetriesAttempts,
        retriesWaitDuration,
        this.indexName,
        this.elasticsearchRestClient,
        AnalysisCentricDocument::getAnalysisId,
        this::mapAnalysisToUpsertRepositoryQuery);
  }

  @Override
  public Mono<Void> removeAnalysisDocs(String analysisId) {
    return Mono.fromSupplier(() -> this.deleteByAnalysisId(analysisId))
        .subscribeOn(Schedulers.elastic());
  }

  @Override
  public Mono<List<AnalysisCentricDocument>> fetchByIds(List<String> ids) {
    log.debug("in fetchByIds, total ids: {} ", ids.size());
    return Mono.fromSupplier(() -> this.fetch(ids)).subscribeOn(Schedulers.elastic());
  }

  @SneakyThrows
  private List<AnalysisCentricDocument> fetch(List<String> ids) {
    return Parallel.blockingScatterGather(ids, MAX_PAGESIZE, this::doFetchByIdQuery).stream()
        .flatMap(List::stream)
        .collect(Collectors.toList());
  }

  @SneakyThrows
  private List<AnalysisCentricDocument> doFetchByIdQuery(Map.Entry<Integer, List<String>> entry) {
    val retry = buildRetry("doFetchByIdQuery", this.maxRetriesAttempts, this.retriesWaitDuration);
    val decorated =
        Retry.<List<AnalysisCentricDocument>>decorateCheckedSupplier(
            retry, () -> getAnalysisCentricDocuments(entry));
    return decorated.apply();
  }

  @SneakyThrows
  private List<AnalysisCentricDocument> getAnalysisCentricDocuments(
      Map.Entry<Integer, List<String>> entry) {
    log.debug("fetch called ids {} ", entry.getValue().size());
    val request = buildMultiGetRequest(entry, this.indexName);
    val result = elasticsearchRestClient.mget(request, RequestOptions.DEFAULT);
    val docs = this.searchResultMapper.mapResults(result, AnalysisCentricDocument.class);
    return List.copyOf(docs);
  }

  @SneakyThrows
  private UpdateRequest mapAnalysisToUpsertRepositoryQuery(
      AnalysisCentricDocument analysisCentricDocument) {
    val mapper = new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    val paramsBuilder = new HashMap<String, Object>();
    paramsBuilder.put(
        "repository",
        mapper.convertValue(analysisCentricDocument.getRepositories().get(0), Map.class));
    paramsBuilder.put("analysis_state", analysisCentricDocument.getAnalysisState());
    paramsBuilder.put("updated_at", getDateIso(analysisCentricDocument.getUpdatedAt()));
    if (analysisCentricDocument.getPublishedAt()
        != null) { // Nullable as may not have been published
      paramsBuilder.put("published_at", getDateIso(analysisCentricDocument.getPublishedAt()));
    }

    val parameters = unmodifiableMap(paramsBuilder);
    val inline = getInline(parameters);

    return new UpdateRequest()
        .id(analysisCentricDocument.getAnalysisId())
        .index(this.indexName)
        .script(inline)
        .upsert(
            new IndexRequest()
                .index(this.indexName)
                .id(analysisCentricDocument.getAnalysisId())
                .source(
                    analysisCentricJSONWriter.writeValueAsString(analysisCentricDocument),
                    XContentType.JSON));
  }

  @Retryable(maxAttempts = 5, backoff = @Backoff(value = 1000, multiplier = 1.5))
  @SneakyThrows
  public void initialize() {
    try {
      val request = new GetIndexRequest(this.indexName);
      val indexExists =
          this.elasticsearchRestClient.indices().exists(request, RequestOptions.DEFAULT);

      log.info(format("Index %s exists? %s", this.indexName, indexExists));
      if (enabled && !indexExists) {
        this.createIndex();
        log.info("index {} has been created", this.indexName);
      }

    } catch (Exception e) {
      // we log here to document the failure if any each attempt.
      log.error("error while initializing ", e);
      // rethrow so a retry happens
      throw e;
    }
  }

  @SneakyThrows
  private void createIndex() {
    val indexSource = loadIndexSourceAsString(this.alias);
    val createIndexRequest = new CreateIndexRequest(this.indexName);
    createIndexRequest.alias(new Alias(this.alias));
    createIndexRequest.source(indexSource, XContentType.JSON);
    this.elasticsearchRestClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);
  }

  @SneakyThrows
  private String loadIndexSourceAsString(String typeName) {
    log.trace("in loadIndexSourceAsString: {}", typeName);
    return inputStreamToString(analysisCentricIndex.getInputStream());
  }

  @SneakyThrows
  private Void deleteByAnalysisId(String analysisId) {
    val retryConfig =
        RetryConfig.custom()
            .maxAttempts(this.maxRetriesAttempts)
            .retryExceptions(IOException.class)
            .waitDuration(Duration.ofMillis(this.retriesWaitDuration))
            .build();
    val retry = Retry.of("deleteByAnalysisId", retryConfig);
    val decorated =
        Retry.decorateCheckedRunnable(retry, () -> this.deleteByAnalysisIdRunnable(analysisId));
    decorated.run();
    return null;
  }

  @SneakyThrows
  private void deleteByAnalysisIdRunnable(@NonNull String analysisId) {
    log.trace("deleteByAnalysisId called, analysis_id {} ", analysisId);
    DeleteByQueryRequest deleteByQueryRequest = new DeleteByQueryRequest(this.alias);
    deleteByQueryRequest.setQuery(
        QueryBuilders.boolQuery().must(QueryBuilders.termQuery("analysis_id", analysisId)));
    this.elasticsearchRestClient.deleteByQuery(deleteByQueryRequest, RequestOptions.DEFAULT);
  }
}
