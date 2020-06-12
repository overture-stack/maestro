package bio.overture.maestro.domain.api;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class IndexPropertiesImpl implements IndexProperties {
  @NonNull private boolean isFileCentricEnabled;
  @NonNull private boolean isAnalysisCentricEnabled;
  @NonNull private String fileCentricIndexName;
  @NonNull private String analysisCentricIndexName;

  @Override
  public String fileCentricIndexName() {
    return fileCentricIndexName;
  }

  @Override
  public String analysisCentricIndexName() {
    return analysisCentricIndexName;
  }
}
