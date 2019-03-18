package bio.overture.maestro.domain.entities.indexer;

import lombok.*;

@Builder
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class IndexFile {
    private String objectId;
    private String fileName;
    private String fileFormat;
    private String fileMd5sum;
    private String repoFileId;
    private Long fileSize;
}
