package bio.overture.maestro.domain.message.in;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.util.List;
import java.util.Map;

@Getter
@Builder
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
    private List<Experiment> experiment;
}
