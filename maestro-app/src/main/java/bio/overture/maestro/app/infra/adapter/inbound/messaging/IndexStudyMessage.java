package bio.overture.maestro.app.infra.adapter.inbound.messaging;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@Getter
@AllArgsConstructor
class IndexStudyMessage {
    @NonNull
    private String studyId;
    @NonNull
    private String repositoryCode;
}
