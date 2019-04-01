package bio.overture.maestro.app.infra.adapter.outbound.indexing.rules;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Elasticsearch related configuration this allows us to keep the beans package private to avoid
 * other packages using them instead of the interface, and be more explicit about configuration scope.
 */
@Configuration
@Import({
   StaticExclusionRulesDAO.class
})
public class ExclusionRulesConfig { }
