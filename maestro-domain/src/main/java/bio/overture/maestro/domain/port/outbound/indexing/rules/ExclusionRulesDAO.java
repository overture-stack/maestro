package bio.overture.maestro.domain.port.outbound.indexing.rules;

import bio.overture.maestro.domain.entities.indexing.rules.ExclusionRule;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public interface ExclusionRulesDAO {
    Mono<Map<Class<?>, List<? extends ExclusionRule>>> getExclusionRules();
}
