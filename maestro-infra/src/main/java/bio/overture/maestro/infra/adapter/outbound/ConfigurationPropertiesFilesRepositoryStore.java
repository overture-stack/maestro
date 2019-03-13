package bio.overture.maestro.infra.adapter.outbound;

import bio.overture.maestro.domain.entities.indexer.FilesRepository;
import bio.overture.maestro.domain.entities.indexer.StorageType;
import bio.overture.maestro.domain.port.outbound.FilesRepositoryStore;
import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;


@Component
@Setter
@Getter
@ConfigurationProperties(prefix = "maestro", ignoreInvalidFields = true)
@NoArgsConstructor
public class ConfigurationPropertiesFilesRepositoryStore implements FilesRepositoryStore {

    private List<PropertiesFileRepository> repositories;

    @Override
    public Mono<FilesRepository> getFilesRepository(String code) {
        return Mono.justOrEmpty(
            repositories.stream()
                .filter(propertiesFileRepository -> propertiesFileRepository.getCode().equalsIgnoreCase(code))
                .distinct()
                .map(this::toFilesRepository)
                .findFirst()
                .orElse(null)
        );
    }

    @Override
    public Flux<FilesRepository> getAll() {
        return Flux.fromIterable(
            repositories.stream()
                .map(this::toFilesRepository)
                .collect(Collectors.toList())
        );
    }

    private FilesRepository toFilesRepository(PropertiesFileRepository propertiesFileRepository) {
        return FilesRepository.builder()
            .code(propertiesFileRepository.getCode())
            .baseUrl(propertiesFileRepository.getUrl())
            .name(propertiesFileRepository.getName())
            .dataPath(propertiesFileRepository.getDataPath())
            .metadataPath(propertiesFileRepository.getMetadataPath())
            .organization(propertiesFileRepository.getOrganization())
            .country(propertiesFileRepository.getCountry())
            .storageType(propertiesFileRepository.getStorageType())
            .build();
    }

    @Data
    private static class PropertiesFileRepository {
        private String name;
        private String code;
        private String url;
        private String dataPath = "/oicr.icgc/data";
        private String metadataPath = "/oicr.icgc.meta/metadata";
        private String organization;
        private String country;
        private StorageType storageType = StorageType.S3;
    }

}
