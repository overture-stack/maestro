package bio.overture.maestro.app.infra.config;

import bio.overture.maestro.app.infra.adapter.inbound.messaging.MessagingConfig;
import bio.overture.maestro.app.infra.adapter.inbound.webapi.GlobalWebExceptionHandler;
import bio.overture.maestro.app.infra.adapter.inbound.webapi.ManagementController;
import bio.overture.maestro.app.infra.adapter.outbound.indexing.elasticsearch.ElasticSearchConfig;
import bio.overture.maestro.app.infra.adapter.outbound.indexing.rules.ExclusionRulesConfig;
import bio.overture.maestro.app.infra.adapter.outbound.metadata.repostiory.RepositoryConfig;
import bio.overture.maestro.app.infra.adapter.outbound.metadata.study.song.SongConfig;
import bio.overture.maestro.app.infra.adapter.outbound.notification.NotificationConfig;
import bio.overture.maestro.app.infra.config.properties.PropertiesConfig;
import bio.overture.maestro.domain.api.DomainApiConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Aggregates all configuration in one place
 */
@Configuration
@Import({
    DomainApiConfig.class,
    PortsConfig.class,
    InfraConfig.class,
})
public class RootConfiguration {
    public final static String ELASTIC_SEARCH_DOCUMENT_JSON_MAPPER = "documentObjectMapper";
}

/**
 * Configuration about domain related beans (ports implementations), delegated here to keep the domain module agnostic of injection framework .
 */
@Configuration
@Import({
    ElasticSearchConfig.class,
    ExclusionRulesConfig.class,
    MessagingConfig.class,
    WebConfig.class,
    SongConfig.class,
    RepositoryConfig.class,
    NotificationConfig.class,
})
class PortsConfig {}

/**
 * Aggregator for all configurations related to
 * the infrastructure beans (I/O & networking, properties, datasources, etc)
 */
@Configuration
@Import({
    PropertiesConfig.class,
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


