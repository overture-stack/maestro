package bio.overture.maestro.infra.config;

import bio.overture.maestro.domain.api.DefaultIndexer;
import bio.overture.maestro.infra.adapter.outbound.ElasticSearchFileIndexRepository;
import bio.overture.maestro.infra.adapter.outbound.SongStudyRepository;
import bio.overture.maestro.infra.adapter.outbound.StaticFilesRepositoryStore;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Configuration
@Import({
    DefaultIndexer.class,
    ElasticSearchFileIndexRepository.class,
    SongStudyRepository.class,
    StaticFilesRepositoryStore.class,
})
public class RootConfiguration {

    static {
        System.setProperty("es.set.netty.runtime.available.processors", "false");
    }

    @Bean
    WebClient webClient() {
        return WebClient.builder()
                .build();
    }

    @Value("${elasticsearch.cluster.name:elasticsearch}")
    private String clusterName;

    @Bean
    public Client client() throws UnknownHostException {
        Settings elasticsearchSettings = Settings.builder()
                .put("client.transport.sniff", true)
                .put("cluster.name", clusterName).build();
        TransportClient client = new PreBuiltTransportClient(elasticsearchSettings);
        client.addTransportAddress(new TransportAddress(InetAddress.getByName("127.0.0.1"), 9300));
        return client;
    }

}
