package bio.overture.maestro.domain.port.outbound.message;

import bio.overture.maestro.domain.entities.indexer.FileCentricDocument;
import lombok.*;

import java.util.List;

@Value
@Builder
@EqualsAndHashCode
public class BatchIndexFilesCommand {
    List<FileCentricDocument> files;
}
