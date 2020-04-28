package bio.overture.maestro.domain.entities.indexing.analysis;

import bio.overture.maestro.domain.entities.indexing.AnalysisType;
import bio.overture.maestro.domain.entities.indexing.Repository;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Builder
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@FieldNameConstants
public class AnalysisCentricDocument {

  @NonNull private String analysisId;

  @NonNull private AnalysisType analysisType;

  @NonNull private String analysisState;

  @NonNull private String studyId;

  @NonNull private List<AnalysisCentricDonor> donors;

  @NonNull private List<AnalysisCentricFile> files;

  @NonNull private List<Repository> repositories;

  private Map<String, Object> experiment;

  @NonNull private final Map<String, Object> data = new TreeMap<>();

  @JsonAnyGetter
  public Map<String, Object> getData() {
    return data;
  }

  @JsonAnySetter
  public void setData(String key, Object value) {
    data.put(key, value);
  }
}
