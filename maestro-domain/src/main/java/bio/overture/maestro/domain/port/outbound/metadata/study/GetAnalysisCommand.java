package bio.overture.maestro.domain.port.outbound.metadata.study;

import lombok.*;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class GetAnalysisCommand {
    @NonNull
    private String analysisId;
    @NonNull
    private String studyId;
    @NonNull
    private String filesRepositoryBaseUrl;
}
