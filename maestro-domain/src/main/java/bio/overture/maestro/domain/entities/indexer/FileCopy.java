package bio.overture.maestro.domain.entities.indexer;

import lombok.*;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class FileCopy {
    private String analysisId;
    private String fileId;
    private String fileName;
    private String fileFormat;
    private String fileMd5sum;
    private long fileSize;
    private Long lastModified;

    private String repoCode;
    private String repoOrg;
    private String repoName;
    private String repoType;
    private String repoCountry;
    private String repoBaseUrl;
    private String repoDataPath;
    private String repoMetadataPath;
    private IndexFile indexFile;

}
