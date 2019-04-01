package bio.overture.maestro.domain.entities.metadata.study;

import bio.overture.maestro.domain.entities.indexing.rules.ExclusionId;
import lombok.*;

import java.util.Map;

/**
 * A Specimen provides information about the source of the sample
 */
@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Specimen {
    @ExclusionId
    private String specimenId;
    private String donorId;
    private String specimenSubmitterId;
    private String specimenClass;
    private String specimenType;
    private Map<String, Object> info;
}
