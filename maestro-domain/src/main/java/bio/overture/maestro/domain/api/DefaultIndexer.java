package bio.overture.maestro.domain.api;

import bio.overture.maestro.domain.api.exception.BadDataException;
import bio.overture.maestro.domain.api.exception.IndexerException;
import bio.overture.maestro.domain.api.exception.NotFoundException;
import bio.overture.maestro.domain.entities.indexer.FilesRepository;
import bio.overture.maestro.domain.port.outbound.message.BatchIndexFilesCommand;
import bio.overture.maestro.domain.port.outbound.message.GetStudyAnalysesCommand;
import bio.overture.maestro.domain.api.message.IndexResult;
import bio.overture.maestro.domain.api.message.IndexStudyCommand;
import bio.overture.maestro.domain.entities.studymetadata.Analysis;
import bio.overture.maestro.domain.entities.indexer.FileCentricDocument;
import bio.overture.maestro.domain.port.outbound.FileDocumentIndexServerAdapter;
import bio.overture.maestro.domain.port.outbound.FilesRepositoryStore;
import bio.overture.maestro.domain.port.outbound.StudyRepository;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

import static bio.overture.maestro.domain.utility.ErrorHandlers.logAndReturn;

@Slf4j
public class DefaultIndexer implements Indexer {

    private final FileDocumentIndexServerAdapter fileDocumentIndexServerAdapter;
    private final StudyRepository studyRepository;
    private final FilesRepositoryStore filesRepositoryStore;

    @Inject
    DefaultIndexer(FileDocumentIndexServerAdapter fileDocumentIndexServerAdapter,
                          StudyRepository studyRepository,
                          FilesRepositoryStore filesRepositoryStore) {
        this.fileDocumentIndexServerAdapter = fileDocumentIndexServerAdapter;
        this.studyRepository = studyRepository;
        this.filesRepositoryStore = filesRepositoryStore;
    }

    @Override
    public Mono<IndexResult> indexStudy(@NonNull IndexStudyCommand indexStudyCommand) {
        return this.filesRepositoryStore.getFilesRepository(indexStudyCommand.getRepositoryCode())
            .switchIfEmpty(Mono.error(new NotFoundException("Repository not found")))
            .flatMap(filesRepository -> getStudyAnalysesAndBuildDocuments(filesRepository, indexStudyCommand))
            .switchIfEmpty(Mono.error(new BadDataException("Empty study " + indexStudyCommand.getStudyId())))
            .flatMap(this::batchIndexFiles)
            .onErrorMap((e) -> logAndReturn(e, "Failed to index study", log));
    }

    @Override
    public void indexAll() {
        throw new IndexerException("Not implemented yet");
    }

    /* ****************
     * Private Methods
     * ***************/

    private List<FileCentricDocument> buildFileDocuments(Analysis analysis, FilesRepository repository) {
        return FileCentricDocumentConverter.fromAnalysis(analysis, repository);
    }

    private Mono<List<FileCentricDocument>> getStudyAnalysesAndBuildDocuments(FilesRepository repo, IndexStudyCommand indexStudyCommand) {
        return getStudyAnalyses(repo, indexStudyCommand)
            .collectList()
            .map(analyses -> buildAnalysisFileDocuments(repo, analyses));
    }

    private Flux<Analysis> getStudyAnalyses(FilesRepository filesRepository, IndexStudyCommand command) {
        return this.studyRepository.getStudyAnalyses(GetStudyAnalysesCommand.builder()
            .filesRepositoryBaseUrl(filesRepository.getBaseUrl())
            .studyId(command.getStudyId())
            .build()
        );
    }

    private List<FileCentricDocument> buildAnalysisFileDocuments(FilesRepository repo, List<Analysis> analyses) {
        return analyses.stream()
            .map(analysis -> buildFileDocuments(analysis, repo))
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    private Mono<IndexResult> batchIndexFiles(List<FileCentricDocument> files) {
        return this.fileDocumentIndexServerAdapter.batchIndexFiles(BatchIndexFilesCommand.builder()
            .files(files)
            .build()
        );
    }
}
