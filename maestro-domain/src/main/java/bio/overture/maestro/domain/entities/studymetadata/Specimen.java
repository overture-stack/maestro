package bio.overture.maestro.domain.entities.studymetadata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Specimen {
    private String specimenId;
    private String donorId;
    private String specimenSubmitterId;
    private String specimenClass;
    private String specimenType;
    private Map<String, Object> info;
}
