package bio.overture.maestro.app.infra.adapter.inbound.messaging;

import lombok.*;

@Value
@ToString
@AllArgsConstructor
class IndexRepositoryMessage {
    @NonNull
    private String repositoryCode;
}
