package bio.overture.maestro.app.infra.adapter.inbound;

import bio.overture.maestor.app.MaestroIntegrationTest;
import bio.overture.maestro.app.infra.config.RootConfiguration;
import bio.overture.maestro.domain.api.Indexer;
import bio.overture.maestro.domain.entities.indexer.FileCentricDocument;
import bio.overture.maestro.domain.entities.metadata.study.Analysis;
import bio.overture.maestro.domain.port.outbound.StudyDAO;
import bio.overture.maestro.domain.port.outbound.message.GetStudyAnalysesCommand;
import bio.overture.maestro.app.infra.adapter.inbound.webapi.ManagementController;
import bio.overture.maestro.app.infra.config.ApplicationProperties;
import bio.overture.masestro.test.TestCategory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.val;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static bio.overture.masestro.test.Fixture.loadJsonFixture;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;


@Tag(TestCategory.INT_TEST)
class ManagementControllerTest extends MaestroIntegrationTest {

    private WebTestClient client;

    @MockBean
    private StudyDAO studyDAO;

    @Autowired
    private Indexer indexer;

    @Autowired
    private ElasticsearchRestTemplate elasticsearchTemplate;

    @Autowired
    ApplicationProperties applicationProperties;

    @Autowired
    @Qualifier(RootConfiguration.ELASTIC_SEARCH_DOCUMENT_JSON_MAPPER)
    ObjectMapper elasticSearchJsonMapper;


    private String alias;

    @BeforeEach
    void setUp() {
        alias = applicationProperties.getFileCentricAlias();
        client = WebTestClient.bindToController(new ManagementController(indexer)).build();
    }

    @Test
    void indexStudy() throws InterruptedException {
        // Given
        val analyses = Mono.just(loadJsonFixture(this.getClass(), "study.json", new TypeReference<List<Analysis>>() {}));
        val expectedDoc0 = loadJsonFixture(this.getClass(), "doc0.json", FileCentricDocument.class, elasticSearchJsonMapper);
        val expectedDoc1 = loadJsonFixture(this.getClass(), "doc1.json", FileCentricDocument.class, elasticSearchJsonMapper);

        given(studyDAO.getStudyAnalyses(any(GetStudyAnalysesCommand.class))).willReturn(analyses);

        // test
        client.post()
            .uri("/index/repository/collab/study/PEME-CA")
            .exchange()
            .expectStatus()
                .isCreated();
        Thread.sleep(2000);

        // assertions
        val query = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
            .withIndices(alias)
            .withTypes(alias)
            .build();

        val page = elasticsearchTemplate.queryForPage(query, FileCentricDocument.class, new CustomSearchResultMapper());
        val docs = page.getContent();

        assertNotNull(docs);
        assertEquals(2L, page.getContent().size());
        assertEquals(expectedDoc1, docs.get(1));
        assertEquals(expectedDoc0, docs.get(0));
    }


    // this is needed because the json in elastic is snake case
    private class CustomSearchResultMapper implements SearchResultMapper {
        @Override
        @SneakyThrows
        public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
            val docs = new ArrayList<T>();
            for(SearchHit hit : response.getHits().getHits()) {
                String source = hit.getSourceAsString();
                val doc = elasticSearchJsonMapper.readValue(source, clazz);
                docs.add(doc);
            }
            float maxScore = response.getHits().getMaxScore();
            return new AggregatedPageImpl<>(docs, pageable, response.getHits().getTotalHits(),
                response.getAggregations(),
                response.getScrollId(),
                maxScore);
        }

        @Override
        public <T> T mapSearchHit(SearchHit searchHit, Class<T> type) {
            return null;
        }

    }
}