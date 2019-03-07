package bio.overture.maestro.domain.port.outbound;

import bio.overture.maestro.domain.entities.FilesRepository;

import java.util.Optional;

public interface FilesRepositoryStore {
    Optional<FilesRepository> getFilesRepository(String code);
}
