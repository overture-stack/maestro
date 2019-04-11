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
    private FailureData failureData;
    private boolean successful;
}
