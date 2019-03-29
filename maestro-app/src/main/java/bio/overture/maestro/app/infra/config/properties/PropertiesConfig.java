package bio.overture.maestro.app.infra.config.properties;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
    DefaultApplicationProperties.class,
})
public class PropertiesConfig {
}
