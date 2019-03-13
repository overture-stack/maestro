package bio.overture.maestro.domain.port.outbound;

import bio.overture.maestro.domain.port.outbound.message.GetStudyAnalysesCommand;
import bio.overture.maestro.domain.entities.studymetadata.Analysis;
import reactor.core.publisher.Flux;

public interface StudyRepository {
    Flux<Analysis> getStudyAnalyses(GetStudyAnalysesCommand getStudyAnalysesCommand);
}
