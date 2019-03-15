package bio.overture.maestro.domain.port.outbound;

import bio.overture.maestro.domain.entities.indexer.FilesRepository;
import lombok.NonNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FilesRepositoryStore {
    Mono<FilesRepository> getFilesRepository(@NonNull String code);
    Flux<FilesRepository> getAll();
}
