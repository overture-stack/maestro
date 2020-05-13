package bio.overture.maestro.domain.port.outbound.indexing;

import bio.overture.maestro.domain.api.message.IndexResult;
import lombok.NonNull;
import reactor.core.publisher.Mono;

public interface AnalysisCentricIndexAdapter  {

  Mono<IndexResult> batchUpsertAnalysisRepositories(@NonNull BatchIndexAnalysisCommand batchIndexAnalysisCommand);

}
