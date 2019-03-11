package bio.overture.maestro.domain.api;

import bio.overture.maestro.domain.entities.FilesRepository;
import bio.overture.maestro.domain.message.out.GetStudyAnalysesCommand;
import bio.overture.maestro.domain.message.in.IndexResult;
import bio.overture.maestro.domain.message.in.IndexStudyCommand;
import bio.overture.maestro.domain.message.out.metadata.Analysis;
import bio.overture.maestro.domain.entities.FileCentricDocument;
import bio.overture.maestro.domain.port.outbound.FileDocumentIndexServerAdapter;
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

    private final FileDocumentIndexServerAdapter fileDocumentIndexServerAdapter;
    private final StudyRepository studyRepository;
    private final FilesRepositoryStore filesRepositoryStore;

    @Inject
    public DefaultIndexer(FileDocumentIndexServerAdapter fileDocumentIndexServerAdapter,
                          StudyRepository studyRepository,
                          FilesRepositoryStore filesRepositoryStore) {
        this.fileDocumentIndexServerAdapter = fileDocumentIndexServerAdapter;
        this.studyRepository = studyRepository;
        this.filesRepositoryStore = filesRepositoryStore;
    }

    @Override
    public Mono<IndexResult> indexStudy(@NonNull IndexStudyCommand indexStudyCommand) {
       return
            this.filesRepositoryStore.getFilesRepository(indexStudyCommand.getRepositoryCode())
            .switchIfEmpty(Mono.error(new RuntimeException("repository not found")))
            .flatMap(repo -> studyRepository.getStudyAnalyses(GetStudyAnalysesCommand.builder()
                        .filesRepositoryBaseUrl(repo.getBaseUrl())
                        .studyId(indexStudyCommand.getStudyId())
                        .build()
                )
                // TODO: remove take
                .take(3)
                .collectList()
                 .map(analyses -> analyses.stream()
                        .map(a -> buildFileDocuments(a, repo))
                        .flatMap(List::stream)
                        .collect(Collectors.toList())
                )
            )
            .switchIfEmpty(Mono.error(new RuntimeException("empty study")))
            .flatMap(fileDocumentIndexServerAdapter::batchIndexFiles)
            .onErrorMap((e) -> {
                log.error("failed to index study", e);
                return new RuntimeException("failed indexing study", e);
            });
    }

    @Override
    public void indexAll() {
        throw new RuntimeException("Not implemented");
    }

    private List<FileCentricDocument> buildFileDocuments(Analysis analysis, FilesRepository repository) {
        return FileCentricDocumentConverter.fromAnalysis(analysis, repository);
    }

}
