package bio.overture.maestro.app.infra.adapter.outbound.indexing.rules;

import bio.overture.maestro.app.infra.config.properties.ApplicationProperties;
import bio.overture.maestro.domain.entities.indexing.rules.ExclusionRule;
import bio.overture.maestro.domain.entities.indexing.rules.IDExclusionRule;
import bio.overture.maestro.domain.entities.metadata.study.*;
import bio.overture.maestro.domain.port.outbound.indexing.rules.ExclusionRulesDAO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.FileNotFoundException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.text.MessageFormat.format;

/**
 * Rules dao backed by application properties as source, it assumes a yaml
 * structure matching the class: {@link RuleConfig}
 */
@Slf4j
public class ApplicationPropertiesExclusionRulesDAO implements ExclusionRulesDAO {

    private final Resource exclusionRulesResource;
    private Map<Class<?>, List<? extends ExclusionRule>> exclusionRules;

    @Inject
    public ApplicationPropertiesExclusionRulesDAO(ApplicationProperties properties) {
        this.exclusionRulesResource = properties.exclusionRules();
    }

    @SneakyThrows
    public Mono<Map<Class<?>, List<? extends ExclusionRule>>> getExclusionRules() {
        return Mono.just(this.exclusionRules);
    }

    @PostConstruct
    public void init() throws Exception {
        val mapper = new ObjectMapper(new YAMLFactory());
        try {
            this.exclusionRulesResource.getFile();
        } catch (FileNotFoundException __) {
            log.warn("No exclusion rules file with name {} found", this.exclusionRulesResource.getFilename());
            return;
        }

        try {
            val ruleConfig = mapper.readValue(this.exclusionRulesResource.getInputStream(), RuleConfig.class);

            if (ruleConfig == null
                || ruleConfig.getById() == null
                || ruleConfig.getById().isEmpty()) {
                this.exclusionRules = Map.of();
                return;
            }

            val rulesByEntity = new LinkedHashMap<Class<?>, List<? extends ExclusionRule>>();
            ruleConfig.getById().forEach((entity, ids) -> {
                if (ids.isEmpty()) return;
                val entityClass = getClassFor(entity);
                rulesByEntity.put(entityClass, List.of(IDExclusionRule.builder()
                        .clazz(entityClass)
                        .ids(ids)
                        .build()
                    )
                );
            });

            this.exclusionRules = Map.copyOf(rulesByEntity);
        } catch (Exception e) {
            this.exclusionRules = Map.of();
            log.error("failed to read exclusion rules", e);
        }
    }

    private Class<?> getClassFor(String entity) {
        switch (entity) {
            case "study": return Study.class;
            case "analysis": return Analysis.class;
            case "file": return File.class;
            case "donor": return Donor.class;
            case "sample": return Sample.class;
            case "specimen" : return Specimen.class;
        }
        throw new IllegalArgumentException(format("entity : {0} is not recognized for exclusion rules", entity));
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class RuleConfig {
        private Map<String, List<String>> byId;
    }
}