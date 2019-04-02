package bio.overture.maestro.app.infra.adapter.outbound.metadata.repostiory;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
    PropertyFileStudyRepositoryDAO.class
})
public class RepositoryConfig {
}
