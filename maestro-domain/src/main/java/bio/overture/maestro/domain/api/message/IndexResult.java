package bio.overture.maestro.domain.api.message;

import bio.overture.maestro.domain.api.exception.FailureData;
import lombok.*;

import java.util.List;

@Getter
@Builder
@ToString
@NoArgsConstructor
@EqualsAndHashCode
@AllArgsConstructor
public class IndexResult {
    private List<FailureData> failures;
    private boolean successful;
}
