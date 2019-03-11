package bio.overture.maestro.domain.entities;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ReferenceGenome {
    private String downloadUrl;
    private String genomeBuild;
    private String referenceName;
}
