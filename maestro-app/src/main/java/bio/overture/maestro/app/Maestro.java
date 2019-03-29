package bio.overture.maestro.app;

import bio.overture.maestro.app.infra.config.RootConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.context.config.annotation.RefreshScope;
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

}
