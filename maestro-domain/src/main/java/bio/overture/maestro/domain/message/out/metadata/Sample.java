package bio.overture.maestro.domain.message.out.metadata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Sample {
    private String sampleId;
    private String specimenId;
    private String sampleSubmitterId;
    private String sampleType;
    private Donor donor;
    private Specimen specimen;
    private Map<String, Object> info;
}
