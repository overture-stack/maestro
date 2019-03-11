package bio.overture.maestro.domain.message.in;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Builder
@Getter
public class IndexStudyCommand {
    @NonNull
    private String studyId;
    @NonNull
    private String repositoryCode;
}
