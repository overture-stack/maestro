package bio.overture.maestro.domain.port.outbound.message;

import lombok.*;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class GetStudyAnalysesCommand {
    private String studyId;
    private String filesRepositoryBaseUrl;
}
