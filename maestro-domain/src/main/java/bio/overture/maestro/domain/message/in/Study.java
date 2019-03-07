package bio.overture.maestro.domain.message.in;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Builder
@Getter
public class Study {
    @NonNull
    private String studyId;
    @NonNull
    private String name;
    @NonNull
    private String organization;
}
