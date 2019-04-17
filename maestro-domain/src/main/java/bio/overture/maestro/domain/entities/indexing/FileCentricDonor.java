package bio.overture.maestro.domain.entities.indexing;

import lombok.*;

@Builder
@Getter
@ToString
@NoArgsConstructor
@EqualsAndHashCode
@AllArgsConstructor
public class FileCentricDonor {
    @NonNull
    private String id;

    @NonNull
    private String submittedId;

    @NonNull
    private Specimen specimen;
}
