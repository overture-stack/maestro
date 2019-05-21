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

package bio.overture.maestro.domain.port.outbound.metadata.study;

import bio.overture.maestro.domain.entities.metadata.study.Analysis;
import bio.overture.maestro.domain.entities.metadata.study.Study;
import lombok.NonNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * The study repository, which provides APIs to read Studies, analyses, etc information from a StudyRepository
 */
public interface StudyDAO {

    /**
     * loads analyses for a single study from a single repository
     *
     * @param getStudyAnalysesCommand contains studyId and repository base url
     * @return a mono of all analyses found related to a study.
     * @throws bio.overture.maestro.domain.api.exception.NotFoundException
     *  in case the study wasn't found.
     */
    @NonNull Mono<List<Analysis>> getStudyAnalyses(@NonNull GetStudyAnalysesCommand getStudyAnalysesCommand);

    /**
     * loads all studies in a repository
     * @param getStudyAnalysesCommand contains repository url
     * @return a flux of all the studies in the specified repository
     */
    @NonNull Flux<Study> getStudies(@NonNull GetAllStudiesCommand getStudyAnalysesCommand);

    /**
     * load an analysis of a study from a repository
     * @param command specifies the analysis id, the study and the repository url
     * @return the analysis mono
     */
    @NonNull Mono<Analysis> getAnalysis(@NonNull GetAnalysisCommand command);
}
