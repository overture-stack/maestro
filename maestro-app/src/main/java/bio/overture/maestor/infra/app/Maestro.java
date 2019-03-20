package bio.overture.maestor.infra.app;

import bio.overture.maestro.infra.adapter.outbound.FileCentricElasticSearchAdapter;
import bio.overture.maestro.infra.config.RootConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.EnableRetry;

@Slf4j
@EnableRetry
@SpringBootApplication
@Import({RootConfiguration.class})
public class Maestro {

    public static void main(String[] args) {
        SpringApplication.run(Maestro.class, args);
    }

    @Bean
    CommandLineRunner commandLineRunner(FileCentricElasticSearchAdapter adapter) {
        return (args) -> adapter.initialize();
    }
}
