package bio.overture.maestro.domain.api.message;

import bio.overture.maestro.domain.api.exception.FailureData;
import lombok.*;

@Getter
@Builder
@ToString
@NoArgsConstructor
@EqualsAndHashCode
@AllArgsConstructor
public class IndexResult {
    @Builder.Default
    private FailureData failureData = FailureData.builder().build();
    private boolean successful;
}
