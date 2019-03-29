package bio.overture.maestro.domain.api;

import bio.overture.maestro.domain.api.message.IndexResult;
import bio.overture.maestro.domain.api.message.IndexStudyCommand;
import bio.overture.maestro.domain.api.message.IndexStudyRepositoryCommand;
import bio.overture.maestro.domain.entities.indexer.FileCentricDocument;
import bio.overture.maestro.domain.entities.indexer.StudyRepository;
import bio.overture.maestro.domain.entities.metadata.study.Analysis;
import bio.overture.maestro.domain.entities.metadata.study.Study;
import bio.overture.maestro.domain.port.outbound.FileCentricIndexAdapter;
import bio.overture.maestro.domain.port.outbound.StudyDAO;
import bio.overture.maestro.domain.port.outbound.StudyRepositoryDAO;
import bio.overture.maestro.domain.port.outbound.message.BatchIndexFilesCommand;
import bio.overture.maestro.domain.port.outbound.message.GetAllStudiesCommand;
import bio.overture.maestro.domain.port.outbound.message.GetStudyAnalysesCommand;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.inject.Inject;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static bio.overture.maestro.domain.utility.Exceptions.badData;
import static bio.overture.maestro.domain.utility.Exceptions.notFound;
import static reactor.core.publisher.Mono.error;

@Slf4j
public class DefaultIndexer implements Indexer {

    private static final String MSG_REPO_NOT_FOUND = "Repository {0} not found";
    private static final String MSG_EMPTY_REPOSITORY = "Empty repository {0}";
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
            .flatMap(filesRepository -> getStudyAnalysesAndBuildDocuments(filesRepository, indexStudyCommand.getStudyId()))
            .flatMap(this::batchUpsert);
    }

    @Override
    public Mono<IndexResult> indexStudyRepository(@NonNull IndexStudyRepositoryCommand indexStudyRepositoryCommand) {
        log.trace("in indexStudyRepository, args: {} ", indexStudyRepositoryCommand);
        return this.studyRepositoryDao.getFilesRepository(indexStudyRepositoryCommand.getRepositoryCode())
            .switchIfEmpty(error(notFound(MSG_REPO_NOT_FOUND, indexStudyRepositoryCommand.getRepositoryCode())))
            .flatMapMany(this::getAllStudies)
            .flatMap(repoAndStudy ->
                getStudyAnalysesAndBuildDocuments(repoAndStudy.getStudyRepository(),
                    repoAndStudy.getStudy().getStudyId()))
            .flatMap(this::batchUpsert)
            .then(Mono.just(IndexResult.builder().successful(true).build()));
    }

    /* ****************
     * Private Methods
     * ***************/

    private Flux<StudyAndRepository> getAllStudies(StudyRepository studyRepository) {
        return this.studyDAO.getStudies(GetAllStudiesCommand.builder()
            .filesRepositoryBaseUrl(studyRepository.getBaseUrl())
            .build()
        )
        .switchIfEmpty(error(badData(MSG_EMPTY_REPOSITORY, studyRepository.getCode())))
        .map(study -> StudyAndRepository.builder()
            .study(study)
            .studyRepository(studyRepository)
            .build()
        );
    }

    private Mono<List<FileCentricDocument>> getStudyAnalysesAndBuildDocuments(StudyRepository repo, String studyId) {
        return getStudyAnalyses(repo.getBaseUrl(), studyId)
            .doOnNext(analyses -> log.debug("loaded {} analyses", analyses.size()))
            .map(analyses -> buildAnalysisFileDocuments(repo, analyses));
    }

    private Mono<List<Analysis>> getStudyAnalyses(String studyRepositoryBaseUrl, String studyId) {
        return this.studyDAO.getStudyAnalyses(GetStudyAnalysesCommand.builder()
            .filesRepositoryBaseUrl(studyRepositoryBaseUrl)
            .studyId(studyId)
            .build()
        )
        .filter(list -> list.size() > 0);
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

    private Mono<IndexResult> batchUpsert(List<FileCentricDocument> files) {
        return this.fileCentricIndexAdapter.batchUpsertFileRepositories(BatchIndexFilesCommand.builder()
            .files(files)
            .build()
        ).doOnSuccess(
            indexResult -> log.trace("finished batchUpsert, list size {}, hashcode {}", files.size(), Objects.hashCode(files))
        );
    }

    @Getter
    @Builder
    private static class StudyAndRepository {
        StudyRepository studyRepository;
        Study study;
    }
}
