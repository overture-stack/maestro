package bio.overture.maestro.app.infra.adapter.outbound;

import bio.overture.maestro.domain.entities.indexer.StudyRepository;
import bio.overture.maestro.domain.entities.indexer.StorageType;
import bio.overture.maestro.domain.port.outbound.StudyRepositoryDAO;
import bio.overture.maestro.app.infra.config.ApplicationProperties;
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
@NoArgsConstructor
@ConfigurationProperties(prefix = ApplicationProperties.MAESTRO_PREFIX, ignoreInvalidFields = true)
public class PropertyFileStudyRepositoryDAO implements StudyRepositoryDAO {

    private List<PropertiesFileRepository> repositories;

    @Override
    public Mono<StudyRepository> getFilesRepository(@NonNull String code) {
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
    public Flux<StudyRepository> getAll() {
        return Flux.fromIterable(
            repositories.stream()
                .map(this::toFilesRepository)
                .collect(Collectors.toList())
        );
    }

    private StudyRepository toFilesRepository(PropertiesFileRepository propertiesFileRepository) {
        log.trace("Converting : {} to StudyRepository ", propertiesFileRepository);
        return StudyRepository.builder()
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
