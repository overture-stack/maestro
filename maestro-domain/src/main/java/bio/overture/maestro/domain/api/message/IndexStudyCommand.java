package bio.overture.maestro.domain.api.message;

import lombok.*;

@Getter
@Builder
@ToString
@NoArgsConstructor
@EqualsAndHashCode
@AllArgsConstructor
public class IndexStudyCommand {
    @NonNull
    private String studyId;
    @NonNull
    private String repositoryCode;
}
