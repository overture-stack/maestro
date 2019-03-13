package bio.overture.maestro.infra.config;

import bio.overture.maestro.domain.api.DefaultIndexer;
import bio.overture.maestro.infra.adapter.outbound.FileCentricElasticSearchAdapter;
import bio.overture.maestro.infra.adapter.outbound.SongStudyRepository;
import bio.overture.maestro.infra.adapter.outbound.ConfigurationPropertiesFilesRepositoryStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@Import({
    DefaultIndexer.class,
    FileCentricElasticSearchAdapter.class,
    SongStudyRepository.class,
    ConfigurationPropertiesFilesRepositoryStore.class,
})
public class RootConfiguration {

    static {
        System.setProperty("es.set.netty.runtime.available.processors", "false");
    }

    @Bean
    WebClient webClient() {
        return WebClient.builder().build();
    }

    @Value("${elasticsearch.cluster.name:elasticsearch}")
    private String clusterName;

}
