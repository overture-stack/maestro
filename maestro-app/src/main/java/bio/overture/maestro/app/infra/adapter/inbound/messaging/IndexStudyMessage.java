package bio.overture.maestro.app.infra.adapter.inbound.messaging;

import lombok.*;

@Value
@ToString
@AllArgsConstructor
class IndexStudyMessage {
    @NonNull
    private String studyId;
    @NonNull
    private String repositoryCode;
}
