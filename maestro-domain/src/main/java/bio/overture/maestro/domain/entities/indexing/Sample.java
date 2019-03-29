package bio.overture.maestro.domain.entities.indexing;

import lombok.*;

import java.util.Map;

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
    private String id;
    private String submittedId;
    private String type;
    private Map<String, Object> info;
}
