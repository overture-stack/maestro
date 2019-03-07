package bio.overture.maestro.domain.message.out;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class IndexFile {
    private String id;
    private String objectId;
    private String fileName;
    private String fileFormat;
    private String fileMd5sum;
    private String repoFileId;
    private Long fileSize;
}
