package bio.overture.maestro.domain.entities.indexer;

import lombok.*;

@Builder
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class FileCentricDonor {
    private String donorId;
    private String donorSubmittedId;
    private String primarySite;
    private Specimen specimen;
}
