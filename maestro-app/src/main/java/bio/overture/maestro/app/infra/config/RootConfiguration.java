package bio.overture.maestro.app.infra.config;

import bio.overture.maestro.domain.api.DefaultIndexer;
import bio.overture.maestro.app.infra.adapter.inbound.webapi.GlobalWebExceptionHandler;
import bio.overture.maestro.app.infra.adapter.inbound.webapi.ManagementController;
import bio.overture.maestro.app.infra.adapter.outbound.PropertyFileStudyRepositoryDAO;
import bio.overture.maestro.app.infra.adapter.outbound.FileCentricElasticSearchAdapter;
import bio.overture.maestro.app.infra.adapter.outbound.SongStudyDAO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import lombok.val;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

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
class WebConfig {}

/**
 * Elasticsearch related configuration
 */
@Configuration
@Import({
    FileCentricElasticSearchAdapter.class,
})
class ElasticSearchClientConfig {

    @Autowired
    ApplicationProperties properties;

    @Bean
    RestHighLevelClient client() {
        val clientConfiguration = ClientConfiguration.builder()
            .connectedTo(properties.getHosts().toArray(new String[0]))
            .build();
        return RestClients.create(clientConfiguration)
            .rest();
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


