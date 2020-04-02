package bio.overture.maestro.app.infra.adapter.outbound.indexing.elasticsearch;

import static bio.overture.maestro.domain.utility.StringUtilities.inputStreamToString;
import static java.lang.String.format;
import static java.util.Collections.singletonMap;

import bio.overture.maestro.app.infra.config.RootConfiguration;
import bio.overture.maestro.app.infra.config.properties.ApplicationProperties;
import bio.overture.maestro.domain.api.exception.FailureData;
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
import io.vavr.control.Try;
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
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.util.Assert;
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
  private static final String ANALYSIS_ID = "analysisId";
  private static final int FALLBACK_MAX_RETRY_ATTEMPTS = 0;
  private static final int FALL_BACK_WAIT_DURATION = 100;
  private final long retriesWaitDuration;

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
    log.debug(
        "in batchUpsertAnalysisRepositories, analyses count: {} ",
        batchIndexAnalysisCommand.getAnalyses().size());
    return Mono.fromSupplier(
            () -> this.bulkUpsertAnalysisRepositories(batchIndexAnalysisCommand.getAnalyses()))
        .subscribeOn(Schedulers.elastic());
  }

  @SneakyThrows
  private IndexResult bulkUpsertAnalysisRepositories(List<AnalysisCentricDocument> analyses) {
    log.trace("in bulkUpsertAnalysisRepositories, analyses count : {} ", analyses.size());
    val failures =
        Parallel.blockingScatterGather(
                analyses, this.documentsPerBulkRequest, this::tryBulkUpsertRequestForPart)
            .stream()
            .flatMap(Set::stream)
            .collect(Collectors.toUnmodifiableSet());
    val fails =
        failures.isEmpty()
            ? FailureData.builder().build()
            : FailureData.builder().failingIds(Map.of(ANALYSIS_ID, failures)).build();
    return IndexResult.builder().failureData(fails).successful(failures.isEmpty()).build();
  }

  @NotNull
  private Set<String> tryBulkUpsertRequestForPart(
      Map.Entry<Integer, List<AnalysisCentricDocument>> entry) {
    val partNum = entry.getKey();
    val listPart = entry.getValue();
    val listPartHash = Objects.hashCode(listPart);
    val retryConfig =
        RetryConfig.custom()
            .maxAttempts(this.maxRetriesAttempts)
            .retryExceptions(IOException.class)
            .waitDuration(Duration.ofMillis(this.retriesWaitDuration))
            .build();
    val retry = Retry.of("tryBulkUpsertRequestForPart", retryConfig);
    val decorated =
        Retry.<Set<String>>decorateCheckedSupplier(
            retry,
            () -> {
              log.trace(
                  "tryBulkUpsertRequestForPart, sending part#: {}, hash: {} ",
                  partNum,
                  listPartHash);
              doRequestForPart(listPart);
              log.trace("tryBulkUpsertRequestForPart: done bulk upsert all docs");
              return Set.of();
            });
    val result =
        Try.of(decorated)
            .recover(
                (t) -> {
                  log.error(
                      "failed sending request for: part#: {}, hash: {} to elastic search,"
                          + " gathering failed Ids.",
                      partNum,
                      listPartHash,
                      t);
                  return listPart.stream()
                      .map(analysisCentricDocument -> analysisCentricDocument.getAnalysisId())
                      .collect(Collectors.toUnmodifiableSet());
                });

    return result.get();
  }

  private void doRequestForPart(List<AnalysisCentricDocument> listPart) throws IOException {
    this.bulkUpdateRequest(
        listPart.stream()
            .map(this::mapAnalysisToUpsertRepositoryQuery)
            .collect(Collectors.toList()));
  }

  @SneakyThrows
  private UpdateRequest mapAnalysisToUpsertRepositoryQuery(
      AnalysisCentricDocument analysisCentricDocument) {
    val mapper = new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    Map<String, Object> parameters =
        singletonMap(
            "repository",
            mapper.convertValue(analysisCentricDocument.getRepositories().get(0), Map.class));
    val inline =
        new Script(
            ScriptType.INLINE,
            "painless",
            "if (!ctx._source.repositories.contains(params.repository)) { ctx._source.repositories.add(params.repository) }",
            parameters);

    return new UpdateRequest()
        .id(analysisCentricDocument.getAnalysisId())
        .index(this.alias)
        .script(inline)
        .upsert(
            new IndexRequest()
                .index(this.alias)
                .id(analysisCentricDocument.getAnalysisId())
                .source(
                    analysisCentricJSONWriter.writeValueAsString(analysisCentricDocument),
                    XContentType.JSON));
  }

  private void bulkUpdateRequest(List<UpdateRequest> requests) throws IOException {
    val bulkRequest = new BulkRequest();
    for (UpdateRequest query : requests) {
      bulkRequest.add(prepareUpdate(query));
    }
    checkForBulkUpdateFailure(
        this.elasticsearchRestClient.bulk(bulkRequest, RequestOptions.DEFAULT));
  }

  private void checkForBulkUpdateFailure(BulkResponse bulkResponse) {
    if (bulkResponse.hasFailures()) {
      val failedDocuments = new HashMap<String, String>();
      for (BulkItemResponse item : bulkResponse.getItems()) {
        if (item.isFailed()) failedDocuments.put(item.getId(), item.getFailureMessage());
      }
      throw new RuntimeException(
          "Bulk indexing has failures. Use ElasticsearchException.getFailedDocuments() for detailed messages ["
              + failedDocuments
              + "]");
    }
  }

  private UpdateRequest prepareUpdate(UpdateRequest req) {
    Assert.notNull(req, "No IndexRequest define for Query");
    String indexName = req.index();
    Assert.notNull(indexName, "No index defined for Query");
    Assert.notNull(req.id(), "No Id define for Query");
    return req;
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
}
