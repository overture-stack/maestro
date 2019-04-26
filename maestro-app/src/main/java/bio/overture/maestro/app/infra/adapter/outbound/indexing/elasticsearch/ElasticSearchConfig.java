package bio.overture.maestro.app.infra.adapter.outbound.indexing.elasticsearch;

import bio.overture.maestro.app.infra.config.properties.ApplicationProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import lombok.val;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.http.HttpHeaders;

import javax.net.ssl.SSLContext;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static bio.overture.maestro.app.infra.config.RootConfiguration.ELASTIC_SEARCH_DOCUMENT_JSON_MAPPER;

/**
 * Elasticsearch related configuration this allows us to keep the beans package private to avoid
 * other packages using them instead of the interface, and be more explicit about configuration scope.
 */
@Configuration
@Import({
    CustomElasticSearchRestAdapter.class,
    FileCentricElasticSearchAdapter.class,
    CustomSearchResultMapper.class
})
public class ElasticSearchConfig {

    /**
     * this bean executes when the application starts it's used to initialize the
     * indexes in elastic search server, can be extended as needed.
     */
    @Bean
    CommandLineRunner elasticsearchBootstrapper(FileCentricElasticSearchAdapter adapter) {
        return (args) -> adapter.initialize();
    }

    @Bean
    RestHighLevelClient client(ApplicationProperties properties) {
        val clientConfiguration = ClientConfiguration.builder()
            .connectedTo(properties.elasticSearchClusterNodes().toArray(new String[0]))
            .build();

        /*
         * This is needed to control the timeouts, by default
         * it's 5 secs, since we send some large bulk requests bumped to 10
         */
        return RestClients.create(new ClientConfiguration() {
            @Override
            public List<InetSocketAddress> getEndpoints() {
                return clientConfiguration.getEndpoints();
            }
            @Override
            public HttpHeaders getDefaultHeaders() {
                return clientConfiguration.getDefaultHeaders();
            }

            @Override
            public boolean useSsl() {
                return clientConfiguration.useSsl();
            }

            @Override
            public Optional<SSLContext> getSslContext() {
                return clientConfiguration.getSslContext();
            }

            @Override
            public Duration getConnectTimeout() {
                return Duration.ofSeconds(properties.elasticSearchClientConnectionTimeoutMillis());
            }

            @Override
            public Duration getSocketTimeout() {
                return Duration.ofMinutes(properties.elasticSearchClientSocketTimeoutMillis());
            }
        }).rest();
    }

    @Bean(name = ELASTIC_SEARCH_DOCUMENT_JSON_MAPPER)
    ObjectMapper documentObjectMapper() {
        val mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        return mapper;
    }

    @Bean
    ElasticsearchRestTemplate elasticsearchRestTemplate(RestHighLevelClient client) {
        return new ElasticsearchRestTemplate(client);
    }
}
