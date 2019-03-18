package bio.overture.maestro.domain.port.outbound.message;

import bio.overture.maestro.domain.entities.indexer.FileCentricDocument;
import lombok.*;

import java.util.List;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class BatchIndexFilesCommand {
    List<FileCentricDocument> files;
}
