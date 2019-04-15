package bio.overture.maestro.domain.port.outbound.indexing;

import bio.overture.maestro.domain.entities.indexing.FileCentricDocument;
import lombok.*;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class BatchIndexFilesCommand {

    @NonNull
    private List<FileCentricDocument> files;

    // avoid dumping all files info as that's too much
    public String toString() {
        val size = files == null ? "null" : String.valueOf(files.size());
        return super.toString() + "[files = " + size + "]";
    }
}
