package bio.overture.maestro.domain.entities.indexing;


import lombok.*;

@Builder
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Repository {
    private String code;
    private String organization;
    private String name;
    private String type;
    private String country;
    private String baseUrl;
    private String dataPath;
    private String metadataPath;
}
