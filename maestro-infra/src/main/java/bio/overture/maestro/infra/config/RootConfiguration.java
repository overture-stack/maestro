package bio.overture.maestro.infra.config;

import bio.overture.maestro.domain.api.DefaultIndexer;
import bio.overture.maestro.infra.adapter.inbound.webapi.GlobalWebExceptionHandler;
import bio.overture.maestro.infra.adapter.inbound.webapi.ManagementController;
import bio.overture.maestro.infra.adapter.outbound.ConfigurationPropertiesFileMetadataRepositoryStore;
import bio.overture.maestro.infra.adapter.outbound.FileCentricElasticSearchAdapter;
import bio.overture.maestro.infra.adapter.outbound.SongFileMetadataRepository;
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

/**
 * Aggregates all configuration in one place
 */
@Configuration
@Import({
    DomainConfig.class,
    InfraConfig.class,

})
public class RootConfiguration {}

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
    SongFileMetadataRepository.class,
    ConfigurationPropertiesFileMetadataRepositoryStore.class,
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

    @Bean
    ElasticsearchRestTemplate elasticsearchRestTemplate(RestHighLevelClient client) {
        return new ElasticsearchRestTemplate(client);
    }
}


