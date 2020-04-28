package bio.overture.maestro.domain.api;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class IndexEnabled implements IndexEnabledProperties {
  @NonNull private boolean isFileCentricEnabled;
  @NonNull private boolean isAnalysisCentricEnabled;
}
