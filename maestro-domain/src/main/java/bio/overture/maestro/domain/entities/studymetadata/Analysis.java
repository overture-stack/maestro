package bio.overture.maestro.domain.entities.studymetadata;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Analysis {
    @NonNull
    private String analysisId;
    @NonNull
    private String analysisType;
    @NonNull
    private String analysisState;
    @NonNull
    private String study;
    private Map<String, Object> info;
    private List<File> file;
    private List<Sample> sample;
    private Map<String, Object> experiment;
}
