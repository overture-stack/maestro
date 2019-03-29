package bio.overture.maestro.app.infra.adapter.outbound.metadata.repostiory;

import bio.overture.maestro.app.infra.config.properties.ApplicationProperties;
import bio.overture.maestro.app.infra.config.properties.PropertiesFileRepository;
import bio.overture.maestro.domain.entities.metadata.repository.StudyRepository;
import bio.overture.maestro.domain.port.outbound.metadata.repository.StudyRepositoryDAO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.inject.Inject;
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
@Getter
@NoArgsConstructor
class PropertyFileStudyRepositoryDAO implements StudyRepositoryDAO {

    private List<PropertiesFileRepository> repositories;

    @Inject
    public PropertyFileStudyRepositoryDAO(ApplicationProperties properties) {
        this.repositories = properties.repositories();
    }

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

}
