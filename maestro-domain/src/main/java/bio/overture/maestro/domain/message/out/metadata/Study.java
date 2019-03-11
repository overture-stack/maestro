package bio.overture.maestro.domain.message.out.metadata;

import lombok.*;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Study {
    @NonNull
    private String studyId;
    @NonNull
    private String name;
    @NonNull
    private String organization;
}
