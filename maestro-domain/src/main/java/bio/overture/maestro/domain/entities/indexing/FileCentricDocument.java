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

}

