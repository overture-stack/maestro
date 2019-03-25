package bio.overture.maestro.domain.entities.indexer;

import lombok.*;

@Builder
@Getter
@ToString
@NoArgsConstructor
@EqualsAndHashCode
@AllArgsConstructor
public class FileCentricDonor {
    private String donorId;
    private String donorSubmittedId;
    private Specimen specimen;
}
