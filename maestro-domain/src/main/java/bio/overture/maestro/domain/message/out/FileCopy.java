package bio.overture.maestro.domain.message.out;

import lombok.Builder;
import lombok.Getter;

import java.util.Set;

@Getter
@Builder
public class FileCopy {
    private String repoDataBundleId;
    private String repoFileId;
    private Set<String> repoDataSetIds;
    private String repoCode;
    private String repoOrg;
    private String repoName;
    private String repoType;
    private String repoCountry;
    private String repoBaseUrl;
    private String repoDataPath;
    private String repoMetadataPath;
    private IndexFile indexFile;
    private String fileName;
    private String fileFormat;
    private String fileMd5sum;
    private long fileSize;
    private long lastModified;

}
