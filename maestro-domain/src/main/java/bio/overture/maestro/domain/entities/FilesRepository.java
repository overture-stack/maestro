package bio.overture.maestro.domain.entities;


import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
@EqualsAndHashCode
public class FilesRepository {
    private String name;
    private String code;
    private String country;
    private String baseUrl;
    private String dataPath;
    private String metadataPath;
    private String storageType;
    private String organization;
}
