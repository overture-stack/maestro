package bio.overture.maestro.domain.entities.indexer;


import lombok.*;

@Builder
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class FilesRepository {
    private String name;
    private String code;
    private String country;
    private String baseUrl;
    private String dataPath;
    private String metadataPath;
    private StorageType storageType;
    private String organization;
}
