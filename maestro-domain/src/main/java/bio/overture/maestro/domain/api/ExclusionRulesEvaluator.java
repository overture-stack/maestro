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
                        Study.builder().studyId(analysis.getStudyId()).build()
                    )
                );
    }

    private static boolean isExcludedByAnalysis(Analysis analysis, Map<Class<?>, List<? extends ExclusionRule>> exclusionRules) {
        return exclusionRules.containsKey(Analysis.class) &&  exclusionRules.get(Analysis.class)
                .stream()
                .anyMatch(r -> r.applies(analysis));
    }

    private static boolean isExcludedByFile(Analysis analysis, Map<Class<?>, List<? extends ExclusionRule>> exclusionRules) {
        return exclusionRules.containsKey(File.class) &&  analysis.getFiles().stream()
            .anyMatch(file -> exclusionRules.get(File.class)
                .stream()
                .anyMatch(r -> r.applies(file))
            );
    }

    private static boolean isExcludedBySample(Analysis analysis, Map<Class<?>, List<? extends ExclusionRule>> exclusionRules) {
        return exclusionRules.containsKey(Sample.class) && analysis.getSamples().stream()
            .anyMatch(sample -> exclusionRules.get(Sample.class)
                .stream()
                .anyMatch(r -> r.applies(sample))
            );
    }

    private static boolean isExcludedBySpecimen(Analysis analysis, Map<Class<?>, List<? extends ExclusionRule>> exclusionRules) {
        return exclusionRules.containsKey(Specimen.class) && analysis.getSamples()
            .stream()
            .map(Sample::getSpecimen)
            .anyMatch(specimen -> exclusionRules.get(Specimen.class)
                .stream()
                .anyMatch(r -> r.applies(specimen))
            );
    }

    private static boolean isExcludedByDonor(Analysis analysis, Map<Class<?>, List<? extends ExclusionRule>> exclusionRules) {
        return exclusionRules.containsKey(Donor.class) && analysis.getSamples()
            .stream()
            .map(Sample::getDonor)
            .anyMatch(donor -> exclusionRules.get(Donor.class)
                .stream()
                .anyMatch(r -> r.applies(donor))
            );
    }

}
