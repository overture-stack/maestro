package bio.overture.maestro.domain.port.outbound;


import bio.overture.maestro.domain.message.IndexResult;
import bio.overture.maestro.domain.message.out.FileDocument;
import lombok.NonNull;
import reactor.core.publisher.Mono;

import java.util.List;

public interface FileIndexRepository {
    Mono<IndexResult> batchIndexFiles(@NonNull List<FileDocument> files);
}
