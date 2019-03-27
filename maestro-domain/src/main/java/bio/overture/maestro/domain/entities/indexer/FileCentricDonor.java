package bio.overture.maestro.domain.entities.indexer;

import lombok.*;

@Builder
@Getter
@ToString
@NoArgsConstructor
@EqualsAndHashCode
@AllArgsConstructor
public class FileCentricDonor {
    private String id;
    private String submittedId;
    private Specimen specimen;
}
