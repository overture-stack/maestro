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

import bio.overture.maestro.domain.api.message.*;
import bio.overture.maestro.domain.entities.indexing.rules.ExclusionRule;
import bio.overture.maestro.domain.port.outbound.indexing.FileCentricIndexAdapter;
import lombok.NonNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Main entry point for the Indexer API
 */
public interface Indexer {

    /**
     * A generic method to index a single analysis to all indices
     * @param indexAnalysisCommand
     * @return failure info and success flag of all indices
     */
    Flux<IndexResult> indexAnalysis(@NonNull IndexAnalysisCommand indexAnalysisCommand);

    /**
     * Used to remove all files documents for an analysis.
     * @param removeAnalysisCommand specify repo studyId and analysis id
     * @return flag indicating success and failure info if any
     */
    Mono<IndexResult> removeAnalysis(@NonNull RemoveAnalysisCommand removeAnalysisCommand);

    /**
     * A generic method to index a study.
     * @param command
     * @return failure info and success flag of all indices
     */
    Flux<IndexResult> indexStudy(@NonNull IndexStudyCommand command);

    /**
     * A generic method to index the entire repository to all indices.
     * @param command contains repository code
     * @return result indicating success/fail and failure information
     */
    Mono<IndexResult> indexRepository(@NonNull IndexStudyRepositoryCommand command);

    void addRule(AddRuleCommand addRuleCommand);
    void deleteRule(DeleteRuleCommand deleteRuleCommand);
    List<? extends ExclusionRule> getAllRules();
}
