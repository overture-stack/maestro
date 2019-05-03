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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.stream.Collectors;


class SnakeCaseJacksonSearchResultMapper implements SearchResultMapper, MultiGetResultMapper {

    private ObjectMapper objectMapper;

    public SnakeCaseJacksonSearchResultMapper(@Qualifier(RootConfiguration.ELASTIC_SEARCH_DOCUMENT_JSON_MAPPER)
                                                  ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    @SneakyThrows
    public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
        val maxScore = response.getHits().getMaxScore();
        val docs = Arrays.stream(response.getHits().getHits())
            .map(hit -> {
                val source = hit.getSourceAsString();
                return objectMapper.readValue(source, clazz);
            }).collect(Collectors.toUnmodifiableList());

        return new AggregatedPageImpl<>(docs,
            pageable,
            response.getHits().getTotalHits(),
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
        Arrays.stream(responses.getResponses())
            .filter((response) -> !response.isFailed() && response.getResponse().isExists())
            .forEach((response) -> {
                T result = objectMapper.readValue(response.getResponse().getSourceAsString(), clazz);
                list.add(result);
            });
        return list;
    }
}