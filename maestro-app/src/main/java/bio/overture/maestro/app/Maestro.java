package bio.overture.maestro.app;

import bio.overture.maestro.app.infra.adapter.outbound.FileCentricElasticSearchAdapter;
import bio.overture.maestro.app.infra.config.RootConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.EnableRetry;

@Slf4j
@EnableRetry
@RefreshScope
@SpringBootApplication
@Import({RootConfiguration.class})
public class Maestro {

    public static void main(String[] args) {
        SpringApplication.run(Maestro.class, args);
    }

    /**
     * this bean executes when the application starts it's used to initialize the
     * indexes in elastic search server, can be extended as needed.
     */
    @Bean
    CommandLineRunner bootstrapper(FileCentricElasticSearchAdapter adapter) {
        return (args) -> adapter.initialize();
    }
}
