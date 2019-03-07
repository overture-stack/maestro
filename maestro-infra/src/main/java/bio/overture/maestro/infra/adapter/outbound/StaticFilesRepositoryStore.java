package bio.overture.maestro.infra.adapter.outbound;

import bio.overture.maestro.domain.entities.FilesRepository;
import bio.overture.maestro.domain.port.outbound.FilesRepositoryStore;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;


@Component
@Setter
@Getter
@ConfigurationProperties(prefix = "maestro", ignoreInvalidFields = true)
@NoArgsConstructor
public class StaticFilesRepositoryStore implements FilesRepositoryStore {

    private String sample;
    private List<Repo> repositories;

    @Override
    public Optional<FilesRepository> getFilesRepository(String code) {
        return  repositories.stream()
                .filter(f -> f.getCode().equals(code))
                .map(repo -> FilesRepository.builder()
                        .code(repo.getCode())
                        .build()
                )
                .findFirst();
    }

    @Data
    public static class Repo {
        private String name;
        private String code;
//    private String country;
//    private String timezone;
//    private String siteUrl;
    private String baseUrl;
//    private String organization;
//    private String description;
//    private String email;
//    private String dataUrl;
//    private String accessUrl;
//    private String metadataUrl;
//    private String registrationUrl;
    }

}
