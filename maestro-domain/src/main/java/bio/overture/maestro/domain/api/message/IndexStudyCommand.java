package bio.overture.maestro.domain.api.message;

import lombok.*;

@Builder
@Getter
@ToString
@EqualsAndHashCode
public class IndexStudyCommand {
    @NonNull
    private String studyId;
    @NonNull
    private String repositoryCode;
}
