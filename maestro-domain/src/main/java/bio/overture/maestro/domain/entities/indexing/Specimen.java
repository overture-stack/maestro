package bio.overture.maestro.domain.entities.indexing;

import lombok.*;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Specimen {
    @NonNull
    private String specimenId;
    @NonNull
    private String type;
    @NonNull
    private String submittedId;
    @NonNull
    private Sample sample;
}

