package bio.overture.maestro.domain.api;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
    DefaultIndexer.class,
    Notifier.class,
})
public class DomainApiConfig {}
