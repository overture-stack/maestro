package bio.overture.maestro.domain.entities.indexing;

import lombok.*;

/**
 * Many samples can belong to an Analysis, a sample represents
 * a donor and a specimen composition.
 */
@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Sample {
    @NonNull
    private String id;
    @NonNull
    private String submittedId;
    @NonNull
    private String type;
}
