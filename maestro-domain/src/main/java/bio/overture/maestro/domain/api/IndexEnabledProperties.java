package bio.overture.maestro.domain.api;

import lombok.NonNull;

public interface IndexEnabledProperties {
  @NonNull boolean isFileCentricEnabled();
  @NonNull boolean isAnalysisCentricEnabled();
}
