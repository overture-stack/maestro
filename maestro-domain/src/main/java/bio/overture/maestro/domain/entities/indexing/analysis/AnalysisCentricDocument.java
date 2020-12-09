package bio.overture.maestro.domain.entities.indexing.analysis;

import bio.overture.maestro.domain.entities.indexing.Donor;
import bio.overture.maestro.domain.entities.indexing.Repository;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.*;
import lombok.experimental.FieldNameConstants;

@Builder
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@FieldNameConstants
public class AnalysisCentricDocument {

  @NonNull private String analysisId;

  @NonNull private String analysisType;

  @NonNull private Integer analysisVersion;

  @NonNull private String analysisState;

  @NonNull private Date updatedAt;

  private Date publishedAt;

  private Date firstPublishedAt;

  @NonNull private String studyId;

  @NonNull private List<Donor> donors;

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

  public void replaceData(Map<String, Object> data) {
    this.data.clear();
    this.data.putAll(data);
  }

  public boolean isValidReplica(AnalysisCentricDocument analysisCentricDocument) {
    if (analysisCentricDocument == null) return false;
    if (this.equals(analysisCentricDocument)) return true;
    return this.analysisId.equals(analysisCentricDocument.getAnalysisId())
        && this.analysisType.equals(analysisCentricDocument.getAnalysisType())
        // FIXME: Might need a rethink of the replica problem as we need to be able to upsert more fields
        // && this.analysisState.equals(analysisCentricDocument.getAnalysisState())
        // && this.analysisVersion.equals(analysisCentricDocument.getAnalysisVersion())
        && this.studyId.equals(analysisCentricDocument.getStudyId())
        && this.donors.equals(analysisCentricDocument.getDonors())
        && this.files.equals(analysisCentricDocument.getFiles());
  }
}
