package bio.overture.maestro.domain.message.in;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Builder
@Getter
public class Sample {
    private String sampleId;
    private String specimenId;
    private String sampleSubmitterId;
    private String sampleType;
    private Donor donor;
    private Specimen specimen;
    private Map<String, Object> info;
}
