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
