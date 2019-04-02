package bio.overture.maestro.domain.entities.indexing;

import lombok.*;

import java.util.Map;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Specimen {
    @NonNull
    private String id;
    @NonNull
    private String type;
    @NonNull
    private String submittedId;
    @NonNull
    private Sample sample;
    private Map<String, Object> info;
}

