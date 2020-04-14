package bio.overture.maestro.app.infra.adapter.outbound.indexing.elasticsearch;

import bio.overture.maestro.domain.api.exception.FailureData;
import bio.overture.maestro.domain.api.message.IndexResult;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.springframework.util.Assert;
import java.io.IOException;
import java.time.Duration;
import java.util.*;

@Slf4j
@NoArgsConstructor
public class SearchAdapterHelper {
    private static final String ANALYSIS_ID = "analysisId";

    public static IndexResult buildIndexResult(@NonNull Set<String> failures, @NonNull String indexName){
        val fails =
                failures.isEmpty()
                        ? FailureData.builder().build()
                        : FailureData.builder().failingIds(Map.of(ANALYSIS_ID, failures)).build();
        return IndexResult.builder()
                .indexName(indexName)
                .failureData(fails).successful(failures.isEmpty()).build();
    }

    public static Retry buildRetry(int maxRetriesAttempts, long retriesWaitDuration){
        val retryConfig =
                RetryConfig.custom()
                        .maxAttempts(maxRetriesAttempts)
                        .retryExceptions(IOException.class)
                        .waitDuration(Duration.ofMillis(retriesWaitDuration))
                        .build();
        val retry = Retry.of("tryBulkUpsertRequestForPart", retryConfig);
        return retry;
    }

    public static BulkRequest buildBulkUpdateRequest(List<UpdateRequest> requests) throws IOException {
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
                        "if (!ctx._source.repositories.contains(params.repository)) { ctx._source.repositories.add(params.repository) }",
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