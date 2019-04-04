package bio.overture.maestro.domain.entities.metadata.study;

import bio.overture.maestro.domain.entities.indexing.rules.ExclusionId;
import lombok.*;

import java.util.Map;

/**
 * Many samples can belong to an Analysis, a sample represents
 * a donor and a specimen composition.
 */
@Getter
@Builder
@ToString
@NoArgsConstructor
@EqualsAndHashCode
@AllArgsConstructor
public class Sample {
    @ExclusionId
    private String sampleId;
    private String specimenId;
    private String sampleSubmitterId;
    private String sampleType;
    private Donor donor;
    private Specimen specimen;
    private Map<String, Object> info;
}
