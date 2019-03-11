package bio.overture.maestro.domain.message.out.metadata;

import lombok.*;

import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Donor {
    @NonNull
    private String donorId;
    @NonNull
    private String donorSubmitterId;
    @NonNull
    private String studyId;
    @NonNull
    private String donorGender;
    private Map<String, Object> info;
}
