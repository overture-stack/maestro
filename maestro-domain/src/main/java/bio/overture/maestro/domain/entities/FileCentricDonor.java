package bio.overture.maestro.domain.entities;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Builder
@Getter
public class FileCentricDonor {
    private String study;
    private String primarySite;
    private String donorId;
    private List<String> specimenId;
    private List<String> specimenType;
    private List<String> sampleId;
    private String donorSubmitterId;
    private List<String> specimenSubmitterId;
    private List<String> sampleSubmitterId;
}
