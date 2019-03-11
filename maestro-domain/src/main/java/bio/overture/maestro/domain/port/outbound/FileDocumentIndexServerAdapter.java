package bio.overture.maestro.domain.port.outbound;


import bio.overture.maestro.domain.message.in.IndexResult;
import bio.overture.maestro.domain.entities.FileCentricDocument;
import lombok.NonNull;
import reactor.core.publisher.Mono;

import java.util.List;

public interface FileDocumentIndexServerAdapter {
    Mono<IndexResult> batchIndexFiles(@NonNull List<FileCentricDocument> files);
}
