package bio.overture.maestro.domain.entities.indexing.analysis;

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
}
