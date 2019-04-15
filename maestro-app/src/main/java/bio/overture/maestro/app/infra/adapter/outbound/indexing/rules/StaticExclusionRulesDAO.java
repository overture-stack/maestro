package bio.overture.maestro.app.infra.adapter.outbound.indexing.rules;

import bio.overture.maestro.domain.entities.indexing.rules.ExclusionRule;
import bio.overture.maestro.domain.entities.indexing.rules.IDExclusionRule;
import bio.overture.maestro.domain.entities.metadata.study.Sample;
import bio.overture.maestro.domain.port.outbound.indexing.rules.ExclusionRulesDAO;
import lombok.SneakyThrows;
import lombok.val;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * static rules dao, good for testing
 */
public class StaticExclusionRulesDAO implements ExclusionRulesDAO {

    @SneakyThrows
    public Mono<Map<Class<?>, List<? extends ExclusionRule>>> getExclusionRules() {
        val clazz = Sample.class;
        return Mono.just(
            Map.of(
                clazz, List.of(new IDExclusionRule(clazz, List.of("SA520221")))
            )
        );
    }

}
