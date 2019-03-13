package bio.overture.maestro.domain.entities.studymetadata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class File {
    private String objectId;
    private String studyId;
    private String analysisId;
    private String fileName;
    private String fileType;
    private String fileMd5sum;
    private String fileAccess;
    private long fileSize;
    private Map<String, Object> info;
}
