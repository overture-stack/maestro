package bio.overture.maestro.domain.entities.indexing;

import lombok.*;

import java.util.List;

/**
 * Represents the structure of the index document that corresponds to an analysis "File".
 */
@Builder
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class FileCentricDocument {

    @NonNull
    private String objectId;

    @NonNull
    private String access;

    @NonNull
    private String study;

    @NonNull
    private FileCentricAnalysis analysis;

    /**
     * The actual genome analysis file information.
     */
    @NonNull
    private File file;

    /**
     * Each file can be hosted in more than one files repository, this references the other repositories (locations)
     * where this file can be fetched from.
     */
    @NonNull
    private List<Repository> repositories;

    @NonNull
    private List<FileCentricDonor> donors;

    /**
     * This method is to check if the file is a valid replica of another file.
     * by replication we mean that an analysis can be copied to a different metadata repository to make downloading
     * the files faster for different geographical locations.
     * it checks all attributes except for the repository (since the repository is expected to be different)
     *
     * @param fileCentricDocument the other file we compare to
     * @return flag indicates if this is a valid replica.
     */
    public boolean isValidReplica(FileCentricDocument fileCentricDocument) {
        if (fileCentricDocument == null) return false;
        if (this.equals(fileCentricDocument)) return false;
        return this.objectId.equals(fileCentricDocument.getObjectId())
            && this.access.equals(fileCentricDocument.getAccess())
            && this.study.equals(fileCentricDocument.getStudy())
            && this.donors.equals(fileCentricDocument.getDonors())
            && this.analysis.equals(fileCentricDocument.getAnalysis())
            && this.file.equals(fileCentricDocument.getFile());

    }
}

