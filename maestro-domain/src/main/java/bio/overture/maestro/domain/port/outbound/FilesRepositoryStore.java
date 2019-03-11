package bio.overture.maestro.domain.port.outbound;

import bio.overture.maestro.domain.entities.FilesRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FilesRepositoryStore {
    Mono<FilesRepository> getFilesRepository(String code);
    Flux<FilesRepository> getAll();
}
