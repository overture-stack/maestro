package bio.overture.maestro.infra.adapter.inbound;

import bio.overture.maestor.infra.app.MaestroIntegrationTest;
import bio.overture.maestro.domain.api.Indexer;
import bio.overture.maestro.domain.entities.studymetadata.Analysis;
import bio.overture.maestro.domain.port.outbound.StudyRepository;
import bio.overture.maestro.domain.port.outbound.message.GetStudyAnalysesCommand;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import static bio.overture.masestro.test.Fixture.loadJsonFixture;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;


class ManagementControllerTest extends MaestroIntegrationTest {

    private WebTestClient client;

    @MockBean
    private StudyRepository studyRepository;

    @Autowired
    private Indexer indexer;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Value("${maestro.elasticsearch.indexes.file-centric.alias}")
    private String alias;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToController(new ManagementController(indexer)).build();
    }

    @Test
    void indexStudy() throws InterruptedException {
        val analyses = Flux.just(loadJsonFixture(this.getClass(), "study.json", Analysis[].class));
        given(studyRepository.getStudyAnalyses(any(GetStudyAnalysesCommand.class))).willReturn(analyses);
        client.post()
            .uri("/index/collab/PEME-CA")
            .exchange()
            .expectStatus()
                .isCreated();
        Thread.sleep(1000);
        SearchQuery query = new NativeSearchQueryBuilder().withQuery(matchAllQuery())
            .withIndices(alias)
            .build();
        long count = elasticsearchTemplate.count(query);
        assertEquals(2L, count);
    }

}