package bio.overture.maestro.domain.message.in;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class IndexResult {
    private boolean successful;
}
