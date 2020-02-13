/*
 *  Copyright (c) 2019. Ontario Institute for Cancer Research
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as
 *   published by the Free Software Foundation, either version 3 of the
 *   License, or (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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

    private Map<Class<?>, List<? extends ExclusionRule>> exclusionRules;

    @Inject
    public ApplicationPropertiesExclusionRulesDAO(ApplicationProperties properties) {
        this.init(properties.idExclusionRules());
    }

    @SneakyThrows
    public Mono<Map<Class<?>, List<? extends ExclusionRule>>> getExclusionRules() {
        return Mono.just(this.exclusionRules);
    }

    private void init(Map<String, List<String>> idExclusionRules) {
        try {
            if (idExclusionRules == null || idExclusionRules.isEmpty()) {
                this.exclusionRules = Map.of();
                return;
            }

            val rulesByEntity = new LinkedHashMap<Class<?>, List<? extends ExclusionRule>>();
            idExclusionRules.forEach((entity, ids) -> {
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
            log.info("loaded exclusionRules :{}", this.exclusionRules.size());
        } catch (Exception e) {
            this.exclusionRules = Map.of();
            log.error("failed to read exclusion rules", e);
        }
    }

    private Class<?> getClassFor(String entity) {
        switch (entity) {
            case "studyId": return Study.class;
            case "analysis": return Analysis.class;
            case "files": return File.class;
            case "donor": return Donor.class;
            case "samples": return Sample.class;
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
