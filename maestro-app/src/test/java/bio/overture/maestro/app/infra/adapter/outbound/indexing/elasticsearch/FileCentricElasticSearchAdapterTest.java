package bio.overture.maestro.app.infra.adapter.outbound.indexing.elasticsearch;

import bio.overture.maestro.app.infra.config.properties.PropertiesConfig;
import bio.overture.maestro.domain.entities.indexing.FileCentricDocument;
import bio.overture.maestro.domain.port.outbound.indexing.BatchIndexFilesCommand;
import bio.overture.masestro.test.Fixture;
import bio.overture.masestro.test.TestCategory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Arrays;

import static bio.overture.maestro.app.infra.config.RootConfiguration.ELASTIC_SEARCH_DOCUMENT_JSON_MAPPER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@Slf4j
@Tag(TestCategory.INT_TEST)
@SpringBootTest(properties = {
    "embedded.elasticsearch.enabled=false"
})
@ContextConfiguration(classes = {FileCentricElasticSearchAdapterTest.Config.class})
class FileCentricElasticSearchAdapterTest {

    @Autowired
    RestHighLevelClient client;

    @Autowired
    FileCentricElasticSearchAdapter adapter;

    @Test
    void shouldRetryOnIOException() throws IOException {
        val files = Arrays.asList(Fixture.loadJsonFixture(
            this.getClass(), "PEME-CA.files.json", FileCentricDocument[].class));
//        given(client.bulk(any(), any(RequestOptions.class))).willThrow(new ConnectException());

        doThrow(ConnectException.class).when(client).bulk(any(), any(RequestOptions.class));

        val result = adapter.batchUpsertFileRepositories(BatchIndexFilesCommand.builder()
            .files(files)
            .build());

        StepVerifier.create(result)
            .expectNext()
            .verifyComplete();
    }

    @Import({
        CustomElasticSearchRestAdapter.class,
        FileCentricElasticSearchAdapter.class,
        PropertiesConfig.class
    })
    @Configuration
    static class Config {

        @Bean
        WebClient webClient() {
            return WebClient.builder().build();
        }

        @Bean
        RestHighLevelClient mockClient() {
            return Mockito.mock(RestHighLevelClient.class, Mockito.RETURNS_DEFAULTS);
        }

        @Bean
        ElasticsearchRestTemplate elasticsearchRestTemplate(RestHighLevelClient client) {
            return new ElasticsearchRestTemplate(client);
        }

        @Bean(name = ELASTIC_SEARCH_DOCUMENT_JSON_MAPPER)
        ObjectMapper documentObjectMapper() {
            val mapper = new ObjectMapper();
            mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
            return mapper;
        }

    }
}