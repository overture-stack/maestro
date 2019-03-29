package bio.overture.maestro.domain.port.outbound.metadata.repository;

import bio.overture.maestro.domain.entities.metadata.repository.StudyRepository;
import lombok.NonNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Provides access to data about the studies repositories that the indexer should read metadata from to get information
 * like: url, repository storage type, urls etc
 */
public interface StudyRepositoryDAO {
    /**
     * Gets a file repository by code
     * @param code the unique code of the repository
     * @return the repository or empty null if not found
     */
    @NonNull Mono<StudyRepository> getFilesRepository(@NonNull String code);
    @NonNull Flux<StudyRepository> getAll();
}
