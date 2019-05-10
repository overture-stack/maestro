package bio.overture.maestro.domain.entities.indexing;

import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.util.Map;

@Builder
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@FieldNameConstants
public class FileCentricAnalysis {

    @NonNull
    private String id;
    @NonNull
    private String type;
    @NonNull
    private String state;
    @NonNull
    private String study;

    private Map<String, Object> experiment;
}
