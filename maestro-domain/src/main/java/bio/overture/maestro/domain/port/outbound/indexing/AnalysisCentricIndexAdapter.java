package bio.overture.maestro.domain.port.outbound.indexing;

import bio.overture.maestro.domain.api.message.IndexResult;
import bio.overture.maestro.domain.entities.indexing.analysis.AnalysisCentricDocument;
import java.util.List;
import lombok.NonNull;
import reactor.core.publisher.Mono;

public interface AnalysisCentricIndexAdapter {

  Mono<IndexResult> batchUpsertAnalysisRepositories(
      @NonNull BatchIndexAnalysisCommand batchIndexAnalysisCommand);

  Mono<Void> removeAnalysisDocs(String analysisId);

  Mono<List<AnalysisCentricDocument>> fetchByIds(List<String> ids);
}
