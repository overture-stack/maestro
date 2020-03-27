package bio.overture.maestro.domain.entities.indexing.analysis;

import bio.overture.maestro.domain.entities.indexing.Sample;
import lombok.*;
import lombok.experimental.FieldNameConstants;

@Builder
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@FieldNameConstants
public class AnalysisCentricSpecimen {

  @NonNull private String id;
  @NonNull private String type;
  @NonNull private String submittedId;
  @NonNull private Sample samples;
  private String tumourNormalDesignation;
  private String specimenTissueSource;

}
