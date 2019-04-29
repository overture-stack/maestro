package bio.overture.maestro.app.infra.adapter.outbound.indexing.elasticsearch;

import bio.overture.maestro.app.infra.config.RootConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.val;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;

import java.util.ArrayList;


class SnakeCaseJacksonSearchResultMapper implements SearchResultMapper {

    private ObjectMapper objectMapper;

    public SnakeCaseJacksonSearchResultMapper(@Qualifier(RootConfiguration.ELASTIC_SEARCH_DOCUMENT_JSON_MAPPER)
                                                  ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    @SneakyThrows
    public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
        val docs = new ArrayList<T>();
        for(SearchHit hit : response.getHits().getHits()) {
            String source = hit.getSourceAsString();
            val doc = objectMapper.readValue(source, clazz);
            docs.add(doc);
        }
        float maxScore = response.getHits().getMaxScore();
        return new AggregatedPageImpl<>(docs, pageable, response.getHits().getTotalHits(),
            response.getAggregations(),
            response.getScrollId(),
            maxScore);
    }

    @Override
    @SneakyThrows
    public <T> T mapSearchHit(SearchHit searchHit, Class<T> type) {
        String source = searchHit.getSourceAsString();
        return objectMapper.readValue(source, type);
    }

}