package bio.overture.maestro.app.infra.adapter.outbound.indexing.elasticsearch;

import bio.overture.maestro.app.infra.config.properties.PropertiesConfig;
import bio.overture.maestro.domain.api.exception.FailureData;
import bio.overture.maestro.domain.api.message.IndexResult;
import bio.overture.maestro.domain.entities.indexing.FileCentricDocument;
import bio.overture.maestro.domain.port.outbound.indexing.BatchIndexFilesCommand;
import bio.overture.masestro.test.Fixture;
import bio.overture.masestro.test.TestCategory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import static bio.overture.maestro.app.infra.config.RootConfiguration.ELASTIC_SEARCH_DOCUMENT_JSON_MAPPER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Slf4j
@Tag(TestCategory.INT_TEST)
@SpringBootTest(properties = {
    "embedded.elasticsearch.enabled=false"
})
@ContextConfiguration(classes = {FileCentricElasticSearchAdapterUnavaibilityTest.Config.class})
class FileCentricElasticSearchAdapterUnavaibilityTest {

    @SpyBean
    private RestHighLevelClient client;

    @Autowired
    private FileCentricElasticSearchAdapter adapter;

    @Test
    void shouldRetryUpsertOnIOException() throws IOException {
        // given
        val files = Arrays.asList(Fixture.loadJsonFixture(
            this.getClass(), "PEME-CA.files.json", FileCentricDocument[].class));

        val expectedResult = IndexResult.builder().failureData(
            FailureData.builder()
                .failingIds(Map.of("analysisId", Set.of("EGAZ00001254368")))
                .build()
        ).successful(false)
        .build();

        // when
        val result = adapter.batchUpsertFileRepositories(BatchIndexFilesCommand.builder()
            .files(files)
            .build());

        //then
        StepVerifier.create(result)
            .expectNext(expectedResult)
            .verifyComplete();

        // since this is a final method I had to add the mockito-extensions directory to test resources
        // see why.md there for more info.
        verify(client, times(3)).bulk(any(BulkRequest.class), any(RequestOptions.class));
    }

    @Import({
        CustomElasticSearchRestAdapter.class,
        FileCentricElasticSearchAdapter.class,
        SnakeCaseJacksonSearchResultMapper.class,
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
            //this will trigger an IO exception
            return RestClients.create(ClientConfiguration.create("non-existing:00000")).rest();
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