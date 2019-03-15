package bio.overture.maestor.infra.app;

import bio.overture.maestro.domain.api.Indexer;
import bio.overture.masestro.test.TestCategory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SpringBootTest(classes = Maestro.class)
//initializes the containers annotated with @Container
@Testcontainers
@ContextConfiguration(classes = Maestro.class, initializers = MaestroIntegrationTest.Initializer.class)
@Tag(TestCategory.INT_TEST)
public class MaestroIntegrationTest {

    @Container
    private static ElasticsearchContainer container =
        new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch-oss:6.6.1");

    // @inject doesn't work
    @Autowired
    private Indexer indexer;

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues values = TestPropertyValues.of(
                "spring.data.elasticsearch.cluster-nodes=" + container.getContainerIpAddress() + ":" + container.getMappedPort(9300)
            );
            values.applyTo(configurableApplicationContext);
        }
    }

    @Test
    void testContext() {
        assertNotNull(indexer);
        assertTrue(container.isRunning());
        System.out.println(container);
    }

}