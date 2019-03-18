package bio.overture.maestro.domain.port.outbound;

import bio.overture.maestro.domain.entities.indexer.FileMetadataRepository;
import lombok.NonNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Provides access to data about the file repositories
 * that the indexer should read metadata from to get information like: url, repository storage type
 */
public interface FileMetadataRepositoryStore {
    /**
     * Gets a file repository by code
     * @param code the unique code of the repository
     * @return the repository or empty null if not found
     */
    Mono<FileMetadataRepository> getFilesRepository(@NonNull String code);
    Flux<FileMetadataRepository> getAll();
}
