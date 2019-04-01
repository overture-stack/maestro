package bio.overture.maestro.domain.entities.metadata.study;

import lombok.*;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Study {
    @NonNull
    private String studyId;
}
