package bio.overture.maestro.domain.api.message;

import lombok.*;

@Getter
@Builder
@ToString
@NoArgsConstructor
@EqualsAndHashCode
@AllArgsConstructor
public class IndexAnalysisCommand {
    @NonNull
    private AnalysisIdentifier analysisIdentifier;
}
