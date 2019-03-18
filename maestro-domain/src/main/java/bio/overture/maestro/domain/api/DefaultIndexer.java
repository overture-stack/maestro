package bio.overture.maestro.domain.api;

import bio.overture.maestro.domain.api.exception.IndexerException;
import bio.overture.maestro.domain.api.message.IndexResult;
import bio.overture.maestro.domain.api.message.IndexStudyCommand;
import bio.overture.maestro.domain.entities.indexer.FileCentricDocument;
import bio.overture.maestro.domain.entities.indexer.FileMetadataRepository;
import bio.overture.maestro.domain.entities.metadata.study.Analysis;
import bio.overture.maestro.domain.port.outbound.FileDocumentIndexingAdapter;
import bio.overture.maestro.domain.port.outbound.FileMetadataRepositoryStore;
import bio.overture.maestro.domain.port.outbound.message.BatchIndexFilesCommand;
import bio.overture.maestro.domain.port.outbound.message.GetStudyAnalysesCommand;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

import static bio.overture.maestro.domain.utility.Exceptions.badData;
import static bio.overture.maestro.domain.utility.Exceptions.notFound;
import static reactor.core.publisher.Mono.error;

@Slf4j
public class DefaultIndexer implements Indexer {

    private static final String MSG_REPO_NOT_FOUND = "Repository {0} not found";
    private static final String EMPTY_STUDY_MSG = "Empty study {0}";
    private final FileDocumentIndexingAdapter fileDocumentIndexingAdapter;
    private final bio.overture.maestro.domain.port.outbound.FileMetadataRepository fileMetadataRepository;
    private final FileMetadataRepositoryStore fileMetadataRepositoryStore;

    @Inject
    DefaultIndexer(FileDocumentIndexingAdapter fileDocumentIndexingAdapter,
                   bio.overture.maestro.domain.port.outbound.FileMetadataRepository fileMetadataRepository,
                   FileMetadataRepositoryStore fileMetadataRepositoryStore) {
        this.fileDocumentIndexingAdapter = fileDocumentIndexingAdapter;
        this.fileMetadataRepository = fileMetadataRepository;
        this.fileMetadataRepositoryStore = fileMetadataRepositoryStore;
    }

    @Override
    public Mono<IndexResult> indexStudy(@NonNull IndexStudyCommand indexStudyCommand) {
        log.trace("in indexStudy, args: {} ", indexStudyCommand);
        return this.fileMetadataRepositoryStore.getFilesRepository(indexStudyCommand.getRepositoryCode())
            .switchIfEmpty(error(notFound(MSG_REPO_NOT_FOUND, indexStudyCommand.getRepositoryCode())))
            .flatMap(filesRepository -> getStudyAnalysesAndBuildDocuments(filesRepository, indexStudyCommand))
            .switchIfEmpty(error(badData(EMPTY_STUDY_MSG, indexStudyCommand.getStudyId())))
            .flatMap(this::batchIndexFiles);
    }

    @Override
    public void indexAll() {
        throw new IndexerException("Not implemented yet");
    }

    /* ****************
     * Private Methods
     * ***************/

    private Mono<List<FileCentricDocument>> getStudyAnalysesAndBuildDocuments(FileMetadataRepository repo,
                                                                              IndexStudyCommand indexStudyCommand) {
        return getStudyAnalyses(repo, indexStudyCommand)
            .collectList()
            .doOnSuccess(analyses -> log.debug("loaded {} analyses", analyses.size()))
            .map(analyses -> buildAnalysisFileDocuments(repo, analyses));
    }

    private Flux<Analysis> getStudyAnalyses(FileMetadataRepository fileMetadataRepository, IndexStudyCommand command) {
        return this.fileMetadataRepository.getStudyAnalyses(GetStudyAnalysesCommand.builder()
            .filesRepositoryBaseUrl(fileMetadataRepository.getBaseUrl())
            .studyId(command.getStudyId())
            .build()
        );
    }

    private List<FileCentricDocument> buildAnalysisFileDocuments(FileMetadataRepository repo, List<Analysis> analyses) {
        return analyses.stream()
            .map(analysis -> buildFileDocuments(analysis, repo))
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    private List<FileCentricDocument> buildFileDocuments(Analysis analysis, FileMetadataRepository repository) {
        return FileCentricDocumentConverter.fromAnalysis(analysis, repository);
    }

    private Mono<IndexResult> batchIndexFiles(List<FileCentricDocument> files) {
        return this.fileDocumentIndexingAdapter.batchIndexFiles(BatchIndexFilesCommand.builder()
            .files(files)
            .build()
        );
    }

}
