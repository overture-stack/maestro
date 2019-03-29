package bio.overture.maestro.domain.entities.indexing;

import lombok.*;

import java.util.Map;

@Builder
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
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
    private Map<String, Object> info;
}
