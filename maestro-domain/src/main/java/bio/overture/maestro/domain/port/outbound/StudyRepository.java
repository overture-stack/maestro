package bio.overture.maestro.domain.port.outbound;

import bio.overture.maestro.domain.message.GetStudyAnalysesCommand;
import bio.overture.maestro.domain.message.in.Analysis;
import reactor.core.publisher.Flux;

public interface StudyRepository {
    Flux<Analysis> getStudyAnalyses(GetStudyAnalysesCommand getStudyAnalysesCommand);
}
