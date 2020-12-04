package bio.overture.maestro.app.infra.adapter.outbound.indexing.elasticsearch;

import bio.overture.maestro.domain.api.exception.FailureData;
import bio.overture.maestro.domain.api.message.IndexResult;
import bio.overture.maestro.domain.utility.Parallel;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.vavr.control.Try;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@NoArgsConstructor
public class SearchAdapterHelper {
  private static final String ANALYSIS_ID = "analysisId";

  public static MultiGetRequest buildMultiGetRequest(
      @NonNull Map.Entry<Integer, List<String>> entry, @NonNull String index) {
    val request = new MultiGetRequest();
    for (String id : entry.getValue()) {
      request.add(new MultiGetRequest.Item(index, id));
    }
    return request;
  }

  public static <T> Mono<IndexResult> batchUpsertDocuments(
      @NonNull List<T> documents,
      int documentsPerBulkRequest,
      int maxRetriesAttempts,
      long retriesWaitDuration,
      String indexName,
      RestHighLevelClient client,
      Function<T, String> documentAnalysisIdExtractor,
      Function<T, UpdateRequest> mapper) {
    log.debug("in batchUpsertAnalysisRepositories, analyses count: {} ", documents.size());
    return Mono.fromSupplier(
            () ->
                bulkUpsertAnalysisRepositories(
                    documents,
                    documentsPerBulkRequest,
                    maxRetriesAttempts,
                    retriesWaitDuration,
                    indexName,
                    client,
                    documentAnalysisIdExtractor,
                    mapper))
        .subscribeOn(Schedulers.elastic());
  }

  @SneakyThrows
  private static <T> IndexResult bulkUpsertAnalysisRepositories(
      List<T> analyses,
      int documentsPerBulkRequest,
      int maxRetriesAttempts,
      long retriesWaitDuration,
      String indexName,
      RestHighLevelClient client,
      Function<T, String> documentAnalysisIdExtractor,
      Function<T, UpdateRequest> mapper) {
    log.trace(
        "in SearchAdapterHelper - bulkUpsertAnalysisRepositories, analyses count : {} ",
        analyses.size());
    val failures =
        Parallel.blockingScatterGather(
                analyses,
                documentsPerBulkRequest,
                (list) ->
                    tryBulkUpsertRequestForPart(
                        list,
                        maxRetriesAttempts,
                        retriesWaitDuration,
                        mapper,
                        documentAnalysisIdExtractor,
                        client))
            .stream()
            .flatMap(Set::stream)
            .collect(Collectors.toUnmodifiableSet());
    return buildIndexResult(failures, indexName);
  }

  @NotNull
  private static <T> Set<String> tryBulkUpsertRequestForPart(
      Map.Entry<Integer, List<T>> entry,
      int maxRetriesAttempts,
      long retriesWaitDuration,
      Function<T, UpdateRequest> mapper,
      Function<T, String> documentAnalysisIdExtractor,
      RestHighLevelClient client) {
    val partNum = entry.getKey();
    val listPart = entry.getValue();
    val listPartHash = Objects.hashCode(listPart);
    val retry = buildRetry("tryBulkUpsertRequestForPart", maxRetriesAttempts, retriesWaitDuration);
    val decorated =
        Retry.<Set<String>>decorateCheckedSupplier(
            retry,
            () -> {
              log.trace(
                  "SearchAdapterHelper - tryBulkUpsertRequestForPart, sending part#: {}, hash: {} ",
                  partNum,
                  listPartHash);
              doRequestForPart(listPart, mapper, client);
              log.trace(
                  "SearchAdapterHelper - tryBulkUpsertRequestForPart: done bulk upsert all docs");
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
                      .map(documentAnalysisIdExtractor)
                      .collect(Collectors.toUnmodifiableSet());
                });

    return result.get();
  }

  private static <T> void doRequestForPart(
      List<T> listPart, Function<T, UpdateRequest> mapper, RestHighLevelClient client)
      throws IOException {
    bulkUpdateRequest(listPart.stream().map(mapper).collect(Collectors.toList()), client);
  }

  private static void bulkUpdateRequest(List<UpdateRequest> requests, RestHighLevelClient client)
      throws IOException {
    val bulkRequest = buildBulkUpdateRequest(requests);
    checkForBulkUpdateFailure(client.bulk(bulkRequest, RequestOptions.DEFAULT));
  }

  public static IndexResult buildIndexResult(
      @NonNull Set<String> failures, @NonNull String indexName) {
    val fails =
        failures.isEmpty()
            ? FailureData.builder().build()
            : FailureData.builder().failingIds(Map.of(ANALYSIS_ID, failures)).build();
    return IndexResult.builder()
        .indexName(indexName)
        .failureData(fails)
        .successful(failures.isEmpty())
        .build();
  }

  public static Retry buildRetry(
      String retryName, int maxRetriesAttempts, long retriesWaitDuration) {
    val retryConfig =
        RetryConfig.custom()
            .maxAttempts(maxRetriesAttempts)
            .retryExceptions(IOException.class)
            .waitDuration(Duration.ofMillis(retriesWaitDuration))
            .build();
    val retry = Retry.of(retryName, retryConfig);
    return retry;
  }

  public static BulkRequest buildBulkUpdateRequest(List<UpdateRequest> requests)
      throws IOException {
    val bulkRequest = new BulkRequest();
    for (UpdateRequest query : requests) {
      bulkRequest.add(prepareUpdate(query));
    }
    return bulkRequest;
  }

  public static void checkForBulkUpdateFailure(BulkResponse bulkResponse) {
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

  public static Script getInline(Map<String, Object> parameters) {
    val inline =
        new Script(
            ScriptType.INLINE,
            "painless",
            "if (!ctx._source.repositories.contains(params.repository)) { ctx._source.repositories.add(params.repository) } \n"
                + "ctx._source.analysis_state = params.analysis_state;\n"
                + "ctx._source.updated_at = ZonedDateTime.parse(params.updated_at).toInstant().toEpochMilli();\n"
                + "ctx._source.published_at = ZonedDateTime.parse(params.published_at).toInstant().toEpochMilli();\n",
            parameters);
    return inline;
  }

  public static Script getInlineForFile(Map<String, Object> parameters) {
    val inline =
        new Script(
            ScriptType.INLINE,
            "painless",
            "if (!ctx._source.repositories.contains(params.repository)) { ctx._source.repositories.add(params.repository) }\n"
                + "ctx._source.analysis.analysis_state = params.analysis_state;\n"
                + "ctx._source.analysis.updated_at = ZonedDateTime.parse(params.updated_at).toInstant().toEpochMilli();\n"
                + "ctx._source.analysis.published_at = ZonedDateTime.parse(params.published_at).toInstant().toEpochMilli();\n",
            parameters);
    return inline;
  }

  private static UpdateRequest prepareUpdate(UpdateRequest req) {
    Assert.notNull(req, "No IndexRequest define for Query");
    String indexName = req.index();
    Assert.notNull(indexName, "No index defined for Query");
    Assert.notNull(req.id(), "No Id define for Query");
    return req;
  }
}
