package bio.overture.maestro.domain.entities;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FilesRepository {
    private String name;
    private String code;
    private String country;
    private String timezone;
    private String siteUrl;
    private String baseUrl;
    private String organization;
    private String description;
    private String email;
    private String dataUrl;
    private String accessUrl;
    private String metadataUrl;
    private String registrationUrl;
}
