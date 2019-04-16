package bio.overture.maestro.app.infra.adapter.inbound.messaging;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;

@Value
@ToString
@AllArgsConstructor
class IndexAnalysisMessage {
    @NonNull
    private String analysisId;
    @NonNull
    private String studyId;
    @NonNull
    private String repositoryCode;
}