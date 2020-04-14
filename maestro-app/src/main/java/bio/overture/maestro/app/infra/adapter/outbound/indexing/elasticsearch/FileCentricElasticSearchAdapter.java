/*
 *  Copyright (c) 2019. Ontario Institute for Cancer Research
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as
 *   published by the Free Software Foundation, either version 3 of the
 *   License, or (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package bio.overture.maestro.app.infra.adapter.outbound.indexing.elasticsearch;

import static bio.overture.maestro.app.infra.adapter.outbound.indexing.elasticsearch.SearchAdapterHelper.*;
import static bio.overture.maestro.domain.utility.StringUtilities.inputStreamToString;
import static java.lang.String.format;
import static java.util.Collections.singletonMap;

import bio.overture.maestro.app.infra.config.RootConfiguration;
import bio.overture.maestro.app.infra.config.properties.ApplicationProperties;
import bio.overture.maestro.domain.api.message.IndexResult;
import bio.overture.maestro.domain.entities.indexing.FileCentricAnalysis;
import bio.overture.maestro.domain.entities.indexing.FileCentricDocument;
import bio.overture.maestro.domain.port.outbound.indexing.BatchIndexFilesCommand;
import bio.overture.maestro.domain.port.outbound.indexing.FileCentricIndexAdapter;
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
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
class FileCentricElasticSearchAdapter implements FileCentricIndexAdapter {

  private static final int FALL_BACK_WAIT_DURATION = 100;
  private static final int FALLBACK_MAX_RETRY_ATTEMPTS = 0;
  private static final int MAX_PAGESIZE = 2000;
  private RestHighLevelClient elasticsearchRestClient;
  private SnakeCaseJacksonSearchResultMapper searchResultMapper;
  private final Resource fileCentricIndex;
  private final String alias;
  private final String indexName;
  private final boolean enabled;
  private final int documentsPerBulkRequest;
  private final int maxRetriesAttempts;
  private final long retriesWaitDuration;
  /** we define a writer to save properties inferring at runtime for each object. */
  private final ObjectWriter fileCentricJSONWriter;

  @Inject
  public FileCentricElasticSearchAdapter(
      RestHighLevelClient elasticsearchRestClient,
      @Qualifier(RootConfiguration.ELASTIC_SEARCH_DOCUMENT_JSON_MAPPER) ObjectMapper objectMapper,
      SnakeCaseJacksonSearchResultMapper searchResultMapper,
      ApplicationProperties properties) {

    this.elasticsearchRestClient = elasticsearchRestClient;
    this.searchResultMapper = searchResultMapper;
    this.fileCentricIndex = properties.fileCentricIndex();
    this.alias = properties.fileCentricAlias();
    this.indexName = properties.fileCentricIndexName();
    this.enabled = properties.isFileCentricIndexEnabled();
    this.documentsPerBulkRequest = properties.maxDocsPerBulkRequest();
    this.fileCentricJSONWriter = objectMapper.writerFor(FileCentricDocument.class);
    this.retriesWaitDuration =
        properties.elasticSearchRetryWaitDurationMillis() > 0
            ? properties.elasticSearchRetryWaitDurationMillis()
            : FALL_BACK_WAIT_DURATION;
    this.maxRetriesAttempts =
        properties.elasticSearchRetryMaxAttempts() >= 0
            ? properties.elasticSearchRetryMaxAttempts()
            : FALLBACK_MAX_RETRY_ATTEMPTS;
  }

  @Override
  public Mono<IndexResult> batchUpsertFileRepositories(
      @NonNull BatchIndexFilesCommand batchIndexFilesCommand) {
    log.debug(
        "in batchUpsertFileRepositories, args: {} ", batchIndexFilesCommand.getFiles().size());
    return Mono.fromSupplier(
            () -> this.bulkUpsertFileRepositories(batchIndexFilesCommand.getFiles()))
        .subscribeOn(Schedulers.elastic());
  }

  @Override
  public Mono<List<FileCentricDocument>> fetchByIds(List<String> ids) {
    log.debug("in fetchByIds, total ids: {} ", ids.size());
    return Mono.fromSupplier(() -> this.fetch(ids)).subscribeOn(Schedulers.elastic());
  }

  @Override
  public Mono<Void> removeFiles(Set<String> ids) {
    log.debug("in removeFiles, ids size: {} ", ids.size());
    if (ids.isEmpty()) {
      return Mono.empty();
    }
    return Mono.fromSupplier(() -> this.deleteByIds(ids)).subscribeOn(Schedulers.elastic());
  }

  @Override
  public Mono<Void> removeAnalysisFiles(String analysisId) {
    return Mono.fromSupplier(() -> this.deleteByAnalysisId(analysisId))
        .subscribeOn(Schedulers.elastic());
  }

  @Retryable(maxAttempts = 5, backoff = @Backoff(value = 1000, multiplier = 1.5))
  @SneakyThrows
  void initialize() {
    try {
      val request = new GetIndexRequest(this.indexName);
      val indexExists =
          this.elasticsearchRestClient.indices().exists(request, RequestOptions.DEFAULT);

      log.info(format("Index %s exists?  %s", this.indexName, indexExists));
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

  @Recover
  void recover(Throwable t) {
    log.error("couldn't initialize the index", t);
  }

  /* *******************
   *  Private methods
   *********************/
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
        Retry.decorateCheckedRunnable(retry, () -> deleteByAnalysisIdRunnable(analysisId));
    decorated.run();
    return null;
  }

  @SneakyThrows
  private void deleteByAnalysisIdRunnable(String analysisId) {
    log.trace("deleteByAnalysisId called, analysisId {} ", analysisId);
    DeleteByQueryRequest deleteByQueryRequest = new DeleteByQueryRequest(this.alias);
    deleteByQueryRequest.setQuery(
        QueryBuilders.boolQuery()
            .must(
                QueryBuilders.termQuery(
                    FileCentricDocument.Fields.analysis + "." + FileCentricAnalysis.Fields.id,
                    analysisId)));
    this.elasticsearchRestClient.deleteByQuery(deleteByQueryRequest, RequestOptions.DEFAULT);
  }

  @SneakyThrows
  private List<FileCentricDocument> fetch(List<String> ids) {
    return Parallel.blockingScatterGather(ids, MAX_PAGESIZE, this::doFetchByIdQuery).stream()
        .flatMap(List::stream)
        .collect(Collectors.toList());
  }

  @SneakyThrows
  private Void deleteByIds(Set<String> ids) {
    val retryConfig =
        RetryConfig.custom()
            .maxAttempts(this.maxRetriesAttempts)
            .retryExceptions(IOException.class)
            .waitDuration(Duration.ofMillis(this.retriesWaitDuration))
            .build();
    val retry = Retry.of("deleteByIds", retryConfig);
    val decorated = Retry.decorateCheckedRunnable(retry, () -> doDeleteByIds(ids));
    decorated.run();
    return null;
  }

  @SneakyThrows
  private void doDeleteByIds(Set<String> ids) {
    log.trace("deleteByIds called, ids {} ", ids);
    val deleteReq = new DeleteByQueryRequest(this.alias);
    deleteReq.setQuery(QueryBuilders.idsQuery().addIds(ids.toArray(new String[] {})));
    this.elasticsearchRestClient.deleteByQuery(deleteReq, RequestOptions.DEFAULT);
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
  private IndexResult bulkUpsertFileRepositories(List<FileCentricDocument> filesList) {
    log.trace("in FileCentricElasticSearchAdapter - bulkUpsertFileRepositories, filesList count : {} ", filesList.size());
    val failures =
        Parallel.blockingScatterGather(
                filesList, this.documentsPerBulkRequest, this::tryBulkUpsertRequestForPart)
            .stream()
            .flatMap(Set::stream)
            .collect(Collectors.toUnmodifiableSet());

    return buildIndexResult(failures, this.indexName);
  }

  @NotNull
  private Set<String> tryBulkUpsertRequestForPart(
      Map.Entry<Integer, List<FileCentricDocument>> entry) {
    val partNum = entry.getKey();
    val listPart = entry.getValue();
    val listPartHash = Objects.hashCode(listPart);
    val retry = buildRetry(this.maxRetriesAttempts, this.retriesWaitDuration);

    val decorated =
        Retry.<Set<String>>decorateCheckedSupplier(
            retry,
            () -> {
              log.trace(
                  "FileCentricElasticSearchAdapter - tryBulkUpsertRequestForPart, sending part#: {}, hash: {} ",
                  partNum,
                  listPartHash);
              doRequestForPart(listPart);
              log.trace("FileCentricElasticSearchAdapter - tryBulkUpsertRequestForPart: done bulk upsert all docs");
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
                      .map(fileCentricDocument -> fileCentricDocument.getAnalysis().getId())
                      .collect(Collectors.toUnmodifiableSet());
                });

    return result.get();
  }

  private void doRequestForPart(List<FileCentricDocument> listPart) throws IOException {
    this.bulkUpdateRequest(
        listPart.stream().map(this::mapFileToUpsertRepositoryQuery).collect(Collectors.toList()));
  }

  private void bulkUpdateRequest(List<UpdateRequest> requests) throws IOException {
   val bulkRequest = buildBulkUpdateRequest(requests);
    checkForBulkUpdateFailure(
        this.elasticsearchRestClient.bulk(bulkRequest, RequestOptions.DEFAULT));
  }

  @SneakyThrows
  private String loadIndexSourceAsString(String typeName) {
    log.trace("in loadIndexSourceAsString: {}", typeName);
    return inputStreamToString(fileCentricIndex.getInputStream());
  }

  @SneakyThrows
  private List<FileCentricDocument> doFetchByIdQuery(Map.Entry<Integer, List<String>> entry) {
    val retryConfig =
        RetryConfig.custom()
            .maxAttempts(this.maxRetriesAttempts)
            .retryExceptions(IOException.class)
            .waitDuration(Duration.ofMillis(this.retriesWaitDuration))
            .build();
    val retry = Retry.of("doFetchByIdQuery", retryConfig);
    val decorated =
        Retry.<List<FileCentricDocument>>decorateCheckedSupplier(
            retry, () -> getFileCentricDocuments(entry));
    return decorated.apply();
  }

  @SneakyThrows
  private List<FileCentricDocument> getFileCentricDocuments(
      Map.Entry<Integer, List<String>> entry) {
    log.debug("fetch called ids {} ", entry.getValue().size());
    val request = new MultiGetRequest();
    for (String id : entry.getValue()) {
      request.add(new MultiGetRequest.Item(this.alias, id));
    }
    val result = elasticsearchRestClient.mget(request, RequestOptions.DEFAULT);
    val docs = this.searchResultMapper.mapResults(result, FileCentricDocument.class);
    return List.copyOf(docs);
  }

  @SneakyThrows
  private UpdateRequest mapFileToUpsertRepositoryQuery(FileCentricDocument fileCentricDocument) {
    val mapper = new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    Map<String, Object> parameters =
        singletonMap(
            "repository",
            mapper.convertValue(fileCentricDocument.getRepositories().get(0), Map.class));

    val inline = getInline(parameters);

    return new UpdateRequest()
        .id(fileCentricDocument.getObjectId())
        .index(this.alias)
        .script(inline)
        .upsert(
            new IndexRequest()
                .index(this.alias)
                .id(fileCentricDocument.getObjectId())
                .source(
                    fileCentricJSONWriter.writeValueAsString(fileCentricDocument),
                    XContentType.JSON));
  }
}
