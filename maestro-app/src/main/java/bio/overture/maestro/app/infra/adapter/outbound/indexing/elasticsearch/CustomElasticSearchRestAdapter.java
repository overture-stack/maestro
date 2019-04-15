package bio.overture.maestro.app.infra.adapter.outbound.indexing.elasticsearch;

import lombok.val;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * This class was created to work around limitation in Spring data elasticsearch rest template in the upsert requests
 * where they drop the update query arguments incorrectly
 *
 * https://jira.spring.io/browse/DATAES-227
 */
class CustomElasticSearchRestAdapter {

    private final ElasticsearchRestTemplate elasticsearchRestTemplate;

    public CustomElasticSearchRestAdapter(ElasticsearchRestTemplate elasticsearchRestTemplate) {
        this.elasticsearchRestTemplate = elasticsearchRestTemplate;
    }

    void bulkUpdateRequest(List<UpdateRequest> requests) throws IOException {
        val bulkRequest = new BulkRequest();
        for (UpdateRequest query : requests) {
            bulkRequest.add(prepareUpdate(query));
        }
        checkForBulkUpdateFailure(this.elasticsearchRestTemplate.getClient().bulk(bulkRequest, RequestOptions.DEFAULT));
    }

    private UpdateRequest prepareUpdate(UpdateRequest req) {
        Assert.notNull(req, "No IndexRequest define for Query");
        String indexName = req.index();
        String type = req.type();
        Assert.notNull(indexName, "No index defined for Query");
        Assert.notNull(type, "No type define for Query");
        Assert.notNull(req.id(), "No Id define for Query");
        return req;
    }

    private void checkForBulkUpdateFailure(BulkResponse bulkResponse) {
        if (bulkResponse.hasFailures()) {
            val failedDocuments = new HashMap<String, String>();
            for (BulkItemResponse item : bulkResponse.getItems()) {
                if (item.isFailed())
                    failedDocuments.put(item.getId(), item.getFailureMessage());
            }
            throw new ElasticsearchException(
                "Bulk indexing has failures. Use ElasticsearchException.getFailedDocuments() for detailed messages ["
                    + failedDocuments + "]",
                failedDocuments);
        }
    }
}
