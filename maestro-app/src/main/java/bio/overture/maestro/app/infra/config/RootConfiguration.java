package bio.overture.maestro.app.infra.config;

import bio.overture.maestro.app.infra.adapter.inbound.webapi.GlobalWebExceptionHandler;
import bio.overture.maestro.app.infra.adapter.inbound.webapi.ManagementController;
import bio.overture.maestro.app.infra.adapter.outbound.FileCentricElasticSearchAdapter;
import bio.overture.maestro.app.infra.adapter.outbound.PropertyFileStudyRepositoryDAO;
import bio.overture.maestro.app.infra.adapter.outbound.SongStudyDAO;
import bio.overture.maestro.domain.api.DefaultIndexer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import lombok.val;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

import javax.inject.Inject;
import javax.net.ssl.SSLContext;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static bio.overture.maestro.app.infra.config.RootConfiguration.ELASTIC_SEARCH_DOCUMENT_JSON_MAPPER;

/**
 * Aggregates all configuration in one place
 */
@Configuration
@Import({
    DomainConfig.class,
    InfraConfig.class,
})
public class RootConfiguration {
    public final static String ELASTIC_SEARCH_DOCUMENT_JSON_MAPPER = "documentObjectMapper";
}

/**
 * Configuration about domain related beans, delegated here to keep the domain module agnostic of injection framework .
 */
@Configuration
@Import({
    DefaultIndexer.class,
})
class DomainConfig {}

/**
 * Aggregator for all configurations related to
 * the infrastructure module (this module).
 */
@Configuration
@Import({
    ElasticSearchClientConfig.class,
    WebConfig.class,
    SongStudyDAO.class,
    PropertyFileStudyRepositoryDAO.class,
    ApplicationProperties.class,
})
class InfraConfig {
    @Bean
    WebClient webClient() {
        return WebClient.builder().build();
    }
}

/**
 * Configuration related to the indexer web api
 */
@Configuration
@Import({
    GlobalWebExceptionHandler.class,
    ManagementController.class,
})
class WebConfig {
    private static final String DEFAULT_DOCUMENT_JSON_MAPPER = "DEFAULT_DOCUMENT_JSON_MAPPER" ;

    /**
     * This bean is needed for spring webflux to not use the ELASTIC_SEARCH_DOCUMENT_JSON_MAPPER
     * marked as primary so by default callers who don't specify which bean they need, will get this.
     */
    @Primary
    @Bean(name = DEFAULT_DOCUMENT_JSON_MAPPER)
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}


/**
 * Elasticsearch related configuration
 */
@Configuration
@Import({
    FileCentricElasticSearchAdapter.class,
})
class ElasticSearchClientConfig {

    @Inject
    private ApplicationProperties properties;

    @Bean
    RestHighLevelClient client() {
        val clientConfiguration = ClientConfiguration.builder()
            .connectedTo(properties.getHosts().toArray(new String[0]))
            .build();


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
                return Duration.ofMinutes(1);
            }

            @Override
            public Duration getSocketTimeout() {
                return Duration.ofMinutes(1);
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
