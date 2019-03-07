package bio.overture.maestro.domain.message.in;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.util.Map;

@Getter
@Builder
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
