package bio.overture.maestro.domain.entities.indexer;

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
    private List<String> study;

    @NonNull
    private FileCentricAnalysis analysis;

    /**
     * Each file can be hosted in more than one files repository, this references the other repositories (locations)
     * where this file can be fetched from.
     */
    @NonNull
    private List<FileCopy> fileCopies;

    @NonNull
    private List<FileCentricDonor> donors;
}
