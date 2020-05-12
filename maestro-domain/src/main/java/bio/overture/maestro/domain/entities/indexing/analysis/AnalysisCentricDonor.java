package bio.overture.maestro.domain.entities.indexing.analysis;

import bio.overture.maestro.domain.entities.indexing.Specimen;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.util.List;

@Builder
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@FieldNameConstants
public class AnalysisCentricDonor {
  @NonNull
  private String id;

  @NonNull
  private String submitterDonorId;

  @NonNull
  private String gender;

  @NonNull
  private List<Specimen> specimens;

}
