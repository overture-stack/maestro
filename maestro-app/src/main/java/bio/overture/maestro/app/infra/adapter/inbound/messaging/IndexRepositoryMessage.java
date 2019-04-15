package bio.overture.maestro.app.infra.adapter.inbound.messaging;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@Getter
@AllArgsConstructor
class IndexRepositoryMessage {
    @NonNull
    private String repositoryCode;
}
