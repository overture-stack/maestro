package bio.overture.maestro.domain.message.out;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class DataBundle {
    private String dataBundleId;
}
