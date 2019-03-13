package bio.overture.maestro.domain.api.message;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@EqualsAndHashCode
@ToString
public class IndexResult {
    private boolean successful;
}
