package bio.overture.maestro.domain.message.in;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.util.Map;

@Getter
@Builder
public class Experiment {
    @NonNull
    private String analysisId;
    @NonNull
    private String variantCallingTool;
    @NonNull
    private String matchedNormalSampleSubmitterId;
    private Map<String, Object> info;
}
