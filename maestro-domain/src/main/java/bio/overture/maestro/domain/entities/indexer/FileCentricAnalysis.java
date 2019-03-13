package bio.overture.maestro.domain.entities.indexer;

import lombok.*;

import java.util.Map;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class FileCentricAnalysis {
    @NonNull
    private String analysisId;
    @NonNull
    private String analysisType;
    @NonNull
    private String analysisState;
    @NonNull
    private String study;
    private Map<String, Object> experiment;
    private Map<String, Object> info;
}
