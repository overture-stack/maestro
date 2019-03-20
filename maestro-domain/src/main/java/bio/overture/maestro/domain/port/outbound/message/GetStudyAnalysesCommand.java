package bio.overture.maestro.domain.port.outbound.message;

import lombok.*;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class GetStudyAnalysesCommand {
    @NonNull private String studyId;
    @NonNull private String filesRepositoryBaseUrl;
}
