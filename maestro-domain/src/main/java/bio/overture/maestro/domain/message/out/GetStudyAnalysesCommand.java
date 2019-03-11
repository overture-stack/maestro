package bio.overture.maestro.domain.message.out;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GetStudyAnalysesCommand {
    private String studyId;
    private String filesRepositoryBaseUrl;
}
