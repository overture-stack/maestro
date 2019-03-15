package bio.overture.maestor.infra.app;

import bio.overture.maestro.infra.config.RootConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({RootConfiguration.class})
public class Maestro {
    public static void main(String[] args) {
        SpringApplication.run(Maestro.class, args);
    }
}
