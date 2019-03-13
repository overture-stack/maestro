package bio.overture.maestro.domain.port.outbound.message;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GetStudyAnalysesCommand {
    private String studyId;
    private String filesRepositoryBaseUrl;
}
