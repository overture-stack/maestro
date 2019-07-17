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
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Main entry point for the Indexer API
 */
public interface Indexer {

    /**
     * Allows indexing a single analysis in a specific metadata repository in a specific study
     * @param indexAnalysisCommand specify repo, study and analysis Id
     * @return success flag and failure info if any
     */
    Mono<IndexResult> indexAnalysis(@NonNull IndexAnalysisCommand indexAnalysisCommand);

    /**
     * Used to remove all file documents for an analysis.
     * @param removeAnalysisCommand specify repo study and analysis id
     * @return flag indicating success and failure info if any
     */
    Mono<IndexResult> removeAnalysis(@NonNull RemoveAnalysisCommand removeAnalysisCommand);
    /**
     * This method will fetch the specified study from the specified repository
     * and will invoke the index server {@link FileCentricIndexAdapter}
     * adapter to batch index the resulting documents.
     *
     * @param indexStudyCommand contains the arguments needed to index a single study.
     * @return an index result indicating success or failure
     * @throws bio.overture.maestro.domain.api.exception.NotFoundException
     *          if the study or the repository are not found
     * @throws bio.overture.maestro.domain.api.exception.BadDataException
     *          if the study is empty or the structure of the study is not as expected
     *          and cannot be used to produce list of {@link bio.overture.maestro.domain.entities.indexing.FileCentricDocument}
     */
    Mono<IndexResult> indexStudy(@NonNull IndexStudyCommand indexStudyCommand);

    /**
     * This method will trigger a full indexing for a metadata repository (all studies)
     * @param indexStudyRepositoryCommand contains the repository code to index.
     * @return result indicating success/fail and failures information.
     */
    Mono<IndexResult> indexStudyRepository(@NonNull IndexStudyRepositoryCommand indexStudyRepositoryCommand);

    void addRule(AddRuleCommand addRuleCommand);
    void deleteRule(DeleteRuleCommand deleteRuleCommand);
    List<? extends ExclusionRule> getAllRules();
}
