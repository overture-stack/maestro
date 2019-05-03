package bio.overture.maestro;

import bio.overture.maestro.app.Maestro;
import bio.overture.maestro.app.infra.config.properties.ApplicationProperties;
import bio.overture.masestro.test.TestCategory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.test.context.ContextConfiguration;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;


/**
 * Base class for full integration tests.
 * it will create an elasticsearch container (using test containers).
 *
 * if you need a more light weight faster tests, avoid using this class, this is meant for full end to end.
 */
@Slf4j
@Tag(TestCategory.INT_TEST)
@ContextConfiguration(classes = {Maestro.class})
@AutoConfigureWireMock(port = 0) // we bind the port in the application.yml ${wiremock.server.port}
@SpringBootTest(classes = {Maestro.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class MaestroIntegrationTest {

    /** wait time for elastic search to update */
    @Value("${maestro.test.elasticsearch.sleep_millis:2500}")
    protected int sleepMillis;

    @Autowired
    private ApplicationProperties properties;

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @AfterEach
    void tearDown() throws InterruptedException {
        // clean indexes after each test to keep tests isolated
        DeleteQuery query = new DeleteQuery();
        query.setIndex(properties.fileCentricAlias());
        query.setType(properties.fileCentricAlias());
        query.setQuery(matchAllQuery());
        elasticsearchRestTemplate.delete(query);
        Thread.sleep(sleepMillis);
    }

}