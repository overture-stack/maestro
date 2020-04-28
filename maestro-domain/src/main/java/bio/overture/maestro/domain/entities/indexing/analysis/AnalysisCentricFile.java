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
public class AnalysisCentricFile  {

  @NonNull private String id;

  @NonNull private String name;

  @NonNull private Long size;

  @NonNull private String type;

  @NonNull private String md5Sum;

  @NonNull private String access;

  @NonNull private String dataType;
}
