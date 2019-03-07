package bio.overture.maestro.domain.message.in;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class Specimen {
    private String specimenId;
    private String donorId;
    private String specimenSubmitterId;
    private String specimenClass;
    private String specimenType;
    private Map<String, Object> info;
}
