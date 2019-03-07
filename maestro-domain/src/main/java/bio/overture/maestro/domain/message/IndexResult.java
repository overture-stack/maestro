package bio.overture.maestro.domain.message;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class IndexResult {
    private boolean successful;
}
