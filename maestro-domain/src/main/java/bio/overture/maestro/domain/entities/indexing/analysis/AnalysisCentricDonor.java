package bio.overture.maestro.domain.entities.indexing.analysis;

import bio.overture.maestro.domain.entities.indexing.Specimen;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldNameConstants;

@Builder
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@FieldNameConstants
public class AnalysisCentricDonor {
  @NonNull private String donorId;

  @NonNull private String submitterDonorId;

  @NonNull private String gender;

  @NonNull private List<Specimen> specimens;
}
