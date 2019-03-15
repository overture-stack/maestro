package bio.overture.maestro.infra.config;

import bio.overture.maestro.domain.api.DefaultIndexer;
import bio.overture.maestro.infra.adapter.inbound.webapi.GlobalWebExceptionHandler;
import bio.overture.maestro.infra.adapter.inbound.webapi.ManagementController;
import bio.overture.maestro.infra.adapter.outbound.ConfigurationPropertiesFilesRepositoryStore;
import bio.overture.maestro.infra.adapter.outbound.FileCentricElasticSearchAdapter;
import bio.overture.maestro.infra.adapter.outbound.SongStudyRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@Import({
    DomainConfig.class,
    WebConfig.class,
})
public class RootConfiguration {
    static {
        System.setProperty("es.set.netty.runtime.available.processors", "false");
    }
}

@Configuration
@Import({
    GlobalWebExceptionHandler.class,
    ManagementController.class
})
class WebConfig{}

@Configuration
@Import({
    DefaultIndexer.class,
    FileCentricElasticSearchAdapter.class,
    SongStudyRepository.class,
    ConfigurationPropertiesFilesRepositoryStore.class,
})
class DomainConfig{
    @Bean
    WebClient webClient() {
        return WebClient.builder().build();
    }
}
