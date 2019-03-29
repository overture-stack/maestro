package bio.overture.maestro.domain.entities.metadata.repository;


import bio.overture.maestro.domain.entities.indexing.StorageType;
import lombok.*;

/**
 * This represents a study (including analyses & files) metadata repository, holds information about sources where this
 * indexer can pull metadata from.
 */
@Builder
@Getter
@ToString
@NoArgsConstructor
@EqualsAndHashCode
@AllArgsConstructor
public class StudyRepository {
    /**
     * display name of the repository
     */
    @NonNull
    private String name;

    /**
     * a unique code for the repository
     */
    @NonNull
    private String code;

    /**
     * the country where this file repository resides
     */
    @NonNull
    private String country;

    /**
     * based url of the host of this repository metadata
     */
    @NonNull
    private String baseUrl;

    /**
     * url path to access the files in the object store
     */
    @NonNull
    private String dataPath;

    /**
     * url path to access metadata about the file.
     */
    private String metadataPath;

    /**
     * the block storage type of files (s3 usually)
     */
    @NonNull
    private StorageType storageType;

    /**
     * the organization the owns this files repository
     */
    @NonNull
    private String organization;
}
