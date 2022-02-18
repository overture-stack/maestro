package bio.overture.maestro.domain.entities.metadata.study;

import java.util.List;
import lombok.*;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class GetAnalysisResponse {
  private List<Analysis> analyses;
  private Integer totalAnalyses;
  private Integer currentTotalAnalyses;
}
