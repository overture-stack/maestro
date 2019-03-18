package bio.overture.maestro.domain.entities.metadata.study;

import lombok.*;

import java.util.Map;

/**
 * A file represents an analysis output that results from the experiment on the Donor specimen.
 * multiple files belong to one Analysis and reside in an object store.
 *
 * A file can reside in multiple repositories and it can have relations to other files in a single analysis
 * like BAM and its index file BAI.
 */
@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
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
