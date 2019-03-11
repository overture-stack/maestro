package bio.overture.maestro.domain.port.outbound;

import bio.overture.maestro.domain.message.out.GetStudyAnalysesCommand;
import bio.overture.maestro.domain.message.out.metadata.Analysis;
import reactor.core.publisher.Flux;

public interface StudyRepository {
    Flux<Analysis> getStudyAnalyses(GetStudyAnalysesCommand getStudyAnalysesCommand);
}
