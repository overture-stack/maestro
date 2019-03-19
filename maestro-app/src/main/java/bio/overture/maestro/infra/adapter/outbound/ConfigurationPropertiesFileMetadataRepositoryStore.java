package bio.overture.maestro.infra.adapter.outbound;

import bio.overture.maestro.domain.entities.indexer.FileMetadataRepository;
import bio.overture.maestro.domain.entities.indexer.StorageType;
import bio.overture.maestro.domain.port.outbound.FileMetadataRepositoryStore;
import bio.overture.maestro.infra.config.ApplicationProperties;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Properties file backed repository store, reads the information by binding
 * to the application.config file property: maestro.repositories
 * the configurable attributes can be found here: {@link PropertiesFileRepository}
 *
 * this serves as a default in memory store, more sophisticated cases may create a DB backed store.
 */
@Slf4j
@Setter
@Getter
@ConfigurationProperties(prefix = ApplicationProperties.MAESTRO_PREFIX, ignoreInvalidFields = true)
@NoArgsConstructor
public class ConfigurationPropertiesFileMetadataRepositoryStore implements FileMetadataRepositoryStore {

    private List<PropertiesFileRepository> repositories;

    @Override
    public Mono<FileMetadataRepository> getFilesRepository(@NonNull String code) {
        val repository = repositories.stream()
            .filter(propertiesFileRepository -> propertiesFileRepository.getCode().equalsIgnoreCase(code))
            .distinct()
            .map(this::toFilesRepository)
            .findFirst()
            .orElse(null);
        log.debug("loaded repository : {}", repository);
        return Mono.justOrEmpty(repository);
    }

    @Override
    public Flux<FileMetadataRepository> getAll() {
        return Flux.fromIterable(
            repositories.stream()
                .map(this::toFilesRepository)
                .collect(Collectors.toList())
        );
    }

    private FileMetadataRepository toFilesRepository(PropertiesFileRepository propertiesFileRepository) {
        log.trace("Converting : {} to FileMetadataRepository", propertiesFileRepository);
        return FileMetadataRepository.builder()
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
    @ToString
    @EqualsAndHashCode
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
