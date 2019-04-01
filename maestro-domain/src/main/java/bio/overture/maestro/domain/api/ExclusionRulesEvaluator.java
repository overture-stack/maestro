package bio.overture.maestro.domain.api;

import bio.overture.maestro.domain.entities.indexing.rules.ExclusionRule;
import bio.overture.maestro.domain.entities.metadata.study.*;

import java.util.List;
import java.util.Map;


class ExclusionRulesEvaluator {

    static boolean shouldExcludeAnalysis(Analysis analysis, Map<Class<?>, List<? extends ExclusionRule>> exclusionRules) {
        return exclusionRules.get(Study.class)
                .stream()
                .anyMatch(r -> r.applies(
                        Study.builder().studyId(analysis.getStudy()).build()
                    )
                )

        || analysis.getFile().stream()
            .anyMatch(file -> exclusionRules.get(File.class)
                .stream()
                .anyMatch(r -> r.applies(file))
            )

        || analysis.getSample().stream()
            .anyMatch(sample -> exclusionRules.get(Sample.class)
                .stream()
                .anyMatch(r -> r.applies(sample))
            )

        || analysis.getSample()
            .stream()
            .map(Sample::getSpecimen)
            .anyMatch(specimen -> exclusionRules.get(Specimen.class)
                .stream()
                .anyMatch(r -> r.applies(specimen))
            )

        || analysis.getSample()
            .stream()
            .map(Sample::getDonor)
            .anyMatch(donor -> exclusionRules.get(Donor.class)
                .stream()
                .anyMatch(r -> r.applies(donor))
            );
    }

}
