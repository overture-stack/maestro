package bio.overture.maestro.domain.entities.indexer;

import lombok.*;

import java.util.List;

@Builder
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
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
