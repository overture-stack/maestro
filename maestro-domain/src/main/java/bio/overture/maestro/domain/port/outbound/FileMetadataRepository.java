package bio.overture.maestro.domain.port.outbound;

import bio.overture.maestro.domain.port.outbound.message.GetStudyAnalysesCommand;
import bio.overture.maestro.domain.entities.metadata.study.Analysis;
import lombok.NonNull;
import reactor.core.publisher.Flux;

/**
 * The api of study repository which represents the source of
 * the metadata about .
 */
public interface FileMetadataRepository {

    /**
     * loads analyses (only published) for a single study from a single repository.
     *
     * @param getStudyAnalysesCommand contains studyId and repository base url
     * @return Flux of all analyses found related to a study.
     * @throws bio.overture.maestro.domain.api.exception.NotFoundException
     *  in case the study wasn't found.
     */
    Flux<Analysis> getStudyAnalyses(@NonNull GetStudyAnalysesCommand getStudyAnalysesCommand);

}
