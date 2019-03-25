package bio.overture.maestro.domain.entities.indexer;

import lombok.*;

import java.util.Map;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Specimen {
    private String id;
    private String type;
    private String submittedId;
    private Sample sample;
    private Map<String, Object> info;
}

