package bio.overture.maestro.domain.entities.indexing.analysis;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
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
public class AnalysisCentricFile {

  @NonNull private String objectId;

  @NonNull private String name;

  @NonNull private Long size;

  @NonNull private String fileType;

  @NonNull private String md5Sum;

  @NonNull private String fileAccess;

  @NonNull private String dataType;

  /**
   * this field is to capture the dynamic fields in the file info. it's the responsibility of the
   * users to make sure the mapping is consistent with the different fields that they want to
   * add/index, they are also responsible to add the mappings of these fields or reindex
   * appropriately.
   */
  @NonNull private final Map<String, Object> info = new TreeMap<>();

  @JsonAnyGetter
  public Map<String, Object> getInfo() {
    return info;
  }

  @JsonAnySetter
  public void setInfo(String key, Object value) {
    info.put(key, value);
  }

  public void replaceInfo(Map<String, Object> data) {
    if (data == null) {
      this.info.clear();
      return;
    }
    this.info.clear();
    this.info.putAll(data);
  }
}
