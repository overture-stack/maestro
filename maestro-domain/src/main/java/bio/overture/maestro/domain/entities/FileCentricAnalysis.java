package bio.overture.maestro.domain.entities;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.util.Map;

@Getter
@Builder
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
