package bio.overture.maestro.domain.port.outbound;


import bio.overture.maestro.domain.api.message.IndexResult;
import bio.overture.maestro.domain.port.outbound.message.BatchIndexFilesCommand;
import reactor.core.publisher.Mono;

public interface FileDocumentIndexServerAdapter {
    Mono<IndexResult> batchIndexFiles(BatchIndexFilesCommand batchIndexFilesCommand);
}
