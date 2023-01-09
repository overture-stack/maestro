package bio.overture.maestro.domain.api;

import lombok.NonNull;

public interface IndexProperties {
  String fileCentricIndexName();

  String analysisCentricIndexName();

  @NonNull
  boolean isFileCentricEnabled();

  @NonNull
  boolean isAnalysisCentricEnabled();
}
