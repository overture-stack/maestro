package bio.overture.maestro.domain.port.outbound.metadata.study;

import bio.overture.maestro.domain.api.exception.IndexerException;
import bio.overture.maestro.domain.entities.metadata.study.Analysis;
import bio.overture.maestro.domain.entities.metadata.study.Study;
import io.vavr.control.Either;
import lombok.NonNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * The study repository, which provides APIs to read Studies, analyses, etc information from a StudyRepository
 */
public interface StudyDAO {

    /**
     * loads analyses (only published) for a single study from a single repository.
     *
     * @param getStudyAnalysesCommand contains studyId and repository base url
     * @return a mono of all analyses found related to a study.
     * @throws bio.overture.maestro.domain.api.exception.NotFoundException
     *  in case the study wasn't found.
     */
    @NonNull Mono<Either<IndexerException, List<Analysis>>> getStudyAnalyses(@NonNull GetStudyAnalysesCommand getStudyAnalysesCommand);
    @NonNull Flux<Either<IndexerException, Study>> getStudies(@NonNull GetAllStudiesCommand getStudyAnalysesCommand);
    @NonNull Mono<Analysis> getAnalysis(@NonNull GetAnalysisCommand command);
}
