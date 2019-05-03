package bio.overture.maestro.app.infra.adapter.outbound.indexing.elasticsearch;

import bio.overture.maestro.app.infra.config.RootConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.val;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.MultiGetResultMapper;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;

import java.util.ArrayList;
import java.util.LinkedList;


class SnakeCaseJacksonSearchResultMapper implements SearchResultMapper, MultiGetResultMapper {

    private ObjectMapper objectMapper;

    public SnakeCaseJacksonSearchResultMapper(@Qualifier(RootConfiguration.ELASTIC_SEARCH_DOCUMENT_JSON_MAPPER)
                                                  ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    @SneakyThrows
    public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
        val docs = new ArrayList<T>();
        for(val hit : response.getHits().getHits()) {
            val source = hit.getSourceAsString();
            val doc = objectMapper.readValue(source, clazz);
            docs.add(doc);
        }
        val maxScore = response.getHits().getMaxScore();
        return new AggregatedPageImpl<>(docs, pageable, response.getHits().getTotalHits(),
            response.getAggregations(),
            response.getScrollId(),
            maxScore);
    }

    @Override
    @SneakyThrows
    public <T> T mapSearchHit(SearchHit searchHit, Class<T> type) {
        val source = searchHit.getSourceAsString();
        return objectMapper.readValue(source, type);
    }

    @Override
    @SneakyThrows
    public <T> LinkedList<T> mapResults(MultiGetResponse responses, Class<T> clazz) {
        val list = new LinkedList<T>();
        for (val response : responses.getResponses()) {
            if (!response.isFailed() && response.getResponse().isExists()) {
                T result = objectMapper.readValue(response.getResponse().getSourceAsString(), clazz);
                list.add(result);
            }
        }
        return list;
    }
}