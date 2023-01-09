package bio.overture.maestro.domain.entities.indexing;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class AnalysisType {
  private String name;
  private Integer version;
}
