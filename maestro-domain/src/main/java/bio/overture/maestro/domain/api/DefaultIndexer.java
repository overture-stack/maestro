package bio.overture.maestro.domain.api;

import bio.overture.maestro.domain.entities.FilesRepository;
import bio.overture.maestro.domain.message.GetStudyAnalysesCommand;
import bio.overture.maestro.domain.message.IndexResult;
import bio.overture.maestro.domain.message.IndexStudyCommand;
import bio.overture.maestro.domain.message.in.Analysis;
import bio.overture.maestro.domain.message.out.FileDocument;
import bio.overture.maestro.domain.port.outbound.FileIndexRepository;
import bio.overture.maestro.domain.port.outbound.FilesRepositoryStore;
import bio.overture.maestro.domain.port.outbound.StudyRepository;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class DefaultIndexer implements Indexer {

    private final FileIndexRepository fileIndexRepository;
    private final StudyRepository studyRepository;
    private final FilesRepositoryStore filesRepositoryStore;

    @Inject
    public DefaultIndexer(FileIndexRepository fileIndexRepository,
                          StudyRepository studyRepository,
                          FilesRepositoryStore filesRepositoryStore) {
        this.fileIndexRepository = fileIndexRepository;
        this.studyRepository = studyRepository;
        this.filesRepositoryStore = filesRepositoryStore;
    }

    @Override
    public Mono<IndexResult> indexStudy(@NonNull IndexStudyCommand indexStudyCommand) {
        FilesRepository filesRepository = this.filesRepositoryStore.getFilesRepository(indexStudyCommand.getRepositoryCode()).orElseThrow(RuntimeException::new);
        return this.studyRepository.getStudyAnalyses(
                GetStudyAnalysesCommand.builder()
                    .filesRepositoryBaseUrl(filesRepository.getCode())
                    .studyId(indexStudyCommand.getStudyId())
                    .build()
             )
            .collectList()
            .map(analysis -> analysis.stream()
                    .map(this::buildFileDocuments)
                    .flatMap(List::stream)
                    .collect(Collectors.toList()))
            .flatMap(fileIndexRepository::batchIndexFiles);
    }


    private List<FileDocument> buildFileDocuments(Analysis analysis) {
        return List.of(FileDocument.builder().build());
    }
}
