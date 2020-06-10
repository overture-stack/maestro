package bio.overture.maestro.domain.port.outbound.indexing;

import bio.overture.maestro.domain.entities.indexing.analysis.AnalysisCentricDocument;
import java.util.List;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class BatchIndexAnalysisCommand {

  @NonNull private List<AnalysisCentricDocument> analyses;

  public String toString() {
    val size = analyses == null ? "null" : String.valueOf(analyses.size());
    return super.toString() + "[analyses = " + size + "]";
  }
}
