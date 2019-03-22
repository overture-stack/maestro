package bio.overture.maestro.domain.api;

import bio.overture.maestro.domain.api.exception.IndexerException;
import bio.overture.maestro.domain.api.message.IndexResult;
import bio.overture.maestro.domain.api.message.IndexStudyCommand;
import bio.overture.maestro.domain.entities.indexer.FileCentricDocument;
import bio.overture.maestro.domain.entities.indexer.StudyRepository;
import bio.overture.maestro.domain.entities.metadata.study.Analysis;
import bio.overture.maestro.domain.port.outbound.FileCentricIndexAdapter;
import bio.overture.maestro.domain.port.outbound.StudyRepositoryDAO;
import bio.overture.maestro.domain.port.outbound.StudyDAO;
import bio.overture.maestro.domain.port.outbound.message.BatchIndexFilesCommand;
import bio.overture.maestro.domain.port.outbound.message.GetStudyAnalysesCommand;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
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
    private static final String MSG_EMPTY_STUDY = "Empty study {0}";
    private final FileCentricIndexAdapter fileCentricIndexAdapter;
    private final StudyDAO studyDAO;
    private final StudyRepositoryDAO studyRepositoryDao;

    @Inject
    DefaultIndexer(FileCentricIndexAdapter fileCentricIndexAdapter,
                   StudyDAO studyDAO,
                   StudyRepositoryDAO studyRepositoryDao) {
        this.fileCentricIndexAdapter = fileCentricIndexAdapter;
        this.studyDAO = studyDAO;
        this.studyRepositoryDao = studyRepositoryDao;
    }

    @Override
    public Mono<IndexResult> indexStudy(@NonNull IndexStudyCommand indexStudyCommand) {
        log.trace("in indexStudy, args: {} ", indexStudyCommand);
        return this.studyRepositoryDao.getFilesRepository(indexStudyCommand.getRepositoryCode())
            .switchIfEmpty(error(notFound(MSG_REPO_NOT_FOUND, indexStudyCommand.getRepositoryCode())))
            .flatMap(filesRepository -> getStudyAnalysesAndBuildDocuments(filesRepository, indexStudyCommand))
            .switchIfEmpty(error(badData(MSG_EMPTY_STUDY, indexStudyCommand.getStudyId())))
            .flatMap(this::batchIndexFiles);
    }

    @Override
    public void indexAll() {
        throw new IndexerException("Not implemented yet");
    }

    /* ****************
     * Private Methods
     * ***************/

    private Mono<List<FileCentricDocument>> getStudyAnalysesAndBuildDocuments(StudyRepository repo,
                                                                              IndexStudyCommand indexStudyCommand) {
        return getStudyAnalyses(repo, indexStudyCommand)
            .doOnSuccess(analyses -> log.debug("loaded {} analyses", analyses.size()))
            .map(analyses -> buildAnalysisFileDocuments(repo, analyses));
    }

    private Mono<List<Analysis>> getStudyAnalyses(StudyRepository studyRepository, IndexStudyCommand command) {
        return this.studyDAO.getStudyAnalyses(GetStudyAnalysesCommand.builder()
            .filesRepositoryBaseUrl(studyRepository.getBaseUrl())
            .studyId(command.getStudyId())
            .build()
        );
    }

    private List<FileCentricDocument> buildAnalysisFileDocuments(StudyRepository repo, List<Analysis> analyses) {
        return analyses.stream()
            .map(analysis -> buildFileDocuments(analysis, repo))
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    private List<FileCentricDocument> buildFileDocuments(Analysis analysis, StudyRepository repository) {
        return FileCentricDocumentConverter.fromAnalysis(analysis, repository);
    }

    private Mono<IndexResult> batchIndexFiles(List<FileCentricDocument> files) {
        return this.fileCentricIndexAdapter.batchIndex(BatchIndexFilesCommand.builder()
            .files(files)
            .build()
        );
    }

}
