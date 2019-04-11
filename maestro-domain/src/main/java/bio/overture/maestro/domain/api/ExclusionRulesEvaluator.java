package bio.overture.maestro.domain.api;

import bio.overture.maestro.domain.entities.indexing.rules.ExclusionRule;
import bio.overture.maestro.domain.entities.metadata.study.*;

import java.util.List;
import java.util.Map;

/**
 * Check analysis entities to see if any is marked for exclusion and hence this whole analysis
 * to be excluded.
 *
 * currently this checks rules in the following order:
 *  - Study
 *  - Analysis
 *  - File
 *  - Sample
 *  - Specimen
 *  - Donor
 *
 */
class ExclusionRulesEvaluator {

    static boolean shouldExcludeAnalysis(Analysis analysis, Map<Class<?>, List<? extends ExclusionRule>> exclusionRules) {
        if (exclusionRules.isEmpty()) return false;
        return isExcludedByStudy(analysis, exclusionRules)
            || isExcludedByAnalysis(analysis, exclusionRules)
            || isExcludedByFile(analysis, exclusionRules)
            || isExcludedBySample(analysis, exclusionRules)
            || isExcludedBySpecimen(analysis, exclusionRules)
            || isExcludedByDonor(analysis, exclusionRules);
    }

    private static boolean isExcludedByStudy(Analysis analysis, Map<Class<?>, List<? extends ExclusionRule>> exclusionRules) {
        return exclusionRules.containsKey(Study.class) && exclusionRules.get(Study.class)
                .stream()
                .anyMatch(r -> r.applies(
                        Study.builder().studyId(analysis.getStudy()).build()
                    )
                );
    }

    private static boolean isExcludedByAnalysis(Analysis analysis, Map<Class<?>, List<? extends ExclusionRule>> exclusionRules) {
        return exclusionRules.containsKey(Analysis.class) &&  exclusionRules.get(Analysis.class)
                .stream()
                .anyMatch(r -> r.applies(analysis));
    }

    private static boolean isExcludedByFile(Analysis analysis, Map<Class<?>, List<? extends ExclusionRule>> exclusionRules) {
        return exclusionRules.containsKey(File.class) &&  analysis.getFile().stream()
            .anyMatch(file -> exclusionRules.get(File.class)
                .stream()
                .anyMatch(r -> r.applies(file))
            );
    }

    private static boolean isExcludedBySample(Analysis analysis, Map<Class<?>, List<? extends ExclusionRule>> exclusionRules) {
        return exclusionRules.containsKey(Sample.class) && analysis.getSample().stream()
            .anyMatch(sample -> exclusionRules.get(Sample.class)
                .stream()
                .anyMatch(r -> r.applies(sample))
            );
    }

    private static boolean isExcludedBySpecimen(Analysis analysis, Map<Class<?>, List<? extends ExclusionRule>> exclusionRules) {
        return exclusionRules.containsKey(Specimen.class) && analysis.getSample()
            .stream()
            .map(Sample::getSpecimen)
            .anyMatch(specimen -> exclusionRules.get(Specimen.class)
                .stream()
                .anyMatch(r -> r.applies(specimen))
            );
    }

    private static boolean isExcludedByDonor(Analysis analysis, Map<Class<?>, List<? extends ExclusionRule>> exclusionRules) {
        return exclusionRules.containsKey(Donor.class) && analysis.getSample()
            .stream()
            .map(Sample::getDonor)
            .anyMatch(donor -> exclusionRules.get(Donor.class)
                .stream()
                .anyMatch(r -> r.applies(donor))
            );
    }

}
