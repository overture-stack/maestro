package bio.overture.maestro;

import bio.overture.maestro.app.Maestro;
import bio.overture.maestro.app.infra.config.properties.ApplicationProperties;
import bio.overture.maestro.domain.api.Indexer;
import bio.overture.masestro.test.TestCategory;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Base class for full integration tests.
 * it will create an elasticsearch container (using test containers).
 *
 * if you need a more light weight faster tests, avoid using this class, this is meant for full end to end.
 */
@SpringBootTest(classes = Maestro.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers //initializes the containers annotated with @Container
@ContextConfiguration(classes = Maestro.class, initializers = MaestroIntegrationTest.RandomPortsInitializer.class)
@Tag(TestCategory.INT_TEST)
@AutoConfigureWireMock(port = 0) // we bind the port in the application.yml ${wiremock.server.port}
public class MaestroIntegrationTest {

    private final static String ELASTIC_SEARCH_CLUSTER_NODES = "maestro.elasticsearch.cluster-nodes";
    private final static String COLLAB_SONG_REPOSITORY = "maestro.repositories[0].url";

    @Container
    private static ElasticsearchContainer container =
        new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch-oss:6.6.1");

    // @inject doesn't work
    @Autowired
    private Indexer indexer;

    @Autowired
    private ApplicationProperties properties;

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @BeforeEach
    void before() {
        System.out.println("before");
    }

    @AfterEach
    void tearDown() {
        // clean indexes after each test to keep tests isolated
        DeleteQuery query = new DeleteQuery();
        query.setIndex(properties.fileCentricAlias());
        query.setType(properties.fileCentricAlias());
        query.setQuery(matchAllQuery());
        elasticsearchRestTemplate.delete(query);
    }

    @Test
    void testContext() {
        assertNotNull(indexer);
        assertTrue(container.isRunning());
        System.out.println(container);
    }

    public static class RandomPortsInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertyValues.of(
                ELASTIC_SEARCH_CLUSTER_NODES + "="
                    + container.getContainerIpAddress() + ":" + container.getMappedPort(9200)
            ).applyTo(applicationContext);
        }
    }
}
