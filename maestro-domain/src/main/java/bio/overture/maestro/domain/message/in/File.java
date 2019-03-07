package bio.overture.maestro.domain.message.in;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class File {
    private String objectId;
    private String studyId;
    private String analysisId;
    private String fileName;
    private String fileType;
    private String fileMd5sum;
    private String fileAccess;
    private long fileSize;
}
