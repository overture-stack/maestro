package bio.overture.maestro.domain.api;

import bio.overture.maestro.domain.api.exception.FailureData;
import bio.overture.maestro.domain.api.exception.IndexerException;
import bio.overture.maestro.domain.api.message.*;
import bio.overture.maestro.domain.entities.indexing.FileCentricDocument;
import bio.overture.maestro.domain.entities.indexing.rules.ExclusionRule;
import bio.overture.maestro.domain.entities.metadata.repository.StudyRepository;
import bio.overture.maestro.domain.entities.metadata.study.Analysis;
import bio.overture.maestro.domain.entities.metadata.study.Study;
import bio.overture.maestro.domain.port.outbound.indexing.BatchIndexFilesCommand;
import bio.overture.maestro.domain.port.outbound.indexing.FileCentricIndexAdapter;
import bio.overture.maestro.domain.port.outbound.indexing.rules.ExclusionRulesDAO;
import bio.overture.maestro.domain.port.outbound.metadata.repository.StudyRepositoryDAO;
import bio.overture.maestro.domain.port.outbound.metadata.study.GetAllStudiesCommand;
import bio.overture.maestro.domain.port.outbound.metadata.study.GetStudyAnalysesCommand;
import bio.overture.maestro.domain.port.outbound.metadata.study.StudyDAO;
import bio.overture.maestro.domain.port.outbound.notification.IndexerNotification;
import io.vavr.control.Either;
import io.vavr.control.Try;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

import static bio.overture.maestro.domain.utility.Exceptions.notFound;
import static bio.overture.maestro.domain.utility.Exceptions.wrapWithIndexerException;
import static java.text.MessageFormat.format;
import static reactor.core.publisher.Mono.error;

@Slf4j
class DefaultIndexer implements Indexer {

    private static final String MSG_REPO_NOT_FOUND = "Repository {0} not found";
    private static final String STUDY_ID = "studyId";
    private static final String REPO_URL = "repoUrl";
    private static final String ERR = "err";
    private static final String REPO_CODE = "repoCode";
    private static final String ANALYSIS_ID = "analysisId";
    private static final String FAILURE_DATA = "failure_data";
    private final FileCentricIndexAdapter fileCentricIndexAdapter;
    private final StudyDAO studyDAO;
    private final StudyRepositoryDAO studyRepositoryDao;
    private final ExclusionRulesDAO exclusionRulesDAO;
    private final Notifier notifier;

    @Inject
    DefaultIndexer(FileCentricIndexAdapter fileCentricIndexAdapter,
                   StudyDAO studyDAO,
                   StudyRepositoryDAO studyRepositoryDao,
                   ExclusionRulesDAO exclusionRulesDAO,
                   Notifier notifier) {
        this.fileCentricIndexAdapter = fileCentricIndexAdapter;
        this.studyDAO = studyDAO;
        this.studyRepositoryDao = studyRepositoryDao;
        this.exclusionRulesDAO = exclusionRulesDAO;
        this.notifier = notifier;
    }

    @Override
    public Mono<IndexResult> indexStudy(@NonNull IndexStudyCommand indexStudyCommand) {
        log.trace("in indexStudy, args: {} ", indexStudyCommand);
        return this.studyRepositoryDao.getFilesRepository(indexStudyCommand.getRepositoryCode())
            .switchIfEmpty(error(notFound(MSG_REPO_NOT_FOUND, indexStudyCommand.getRepositoryCode())))
            .map(filesRepository -> toStudyAndRepositoryEither(indexStudyCommand, filesRepository))
            .flatMap(this::getStudyAnalysesDocuments)
            .flatMap(this::batchUpsertFiles);
    }

    @Override
    public Mono<IndexResult> indexStudyRepository(@NonNull IndexStudyRepositoryCommand indexStudyRepositoryCommand) {
        log.trace("in indexStudyRepository, args: {} ", indexStudyRepositoryCommand);
        return this.studyRepositoryDao.getFilesRepository(indexStudyRepositoryCommand.getRepositoryCode())
            .switchIfEmpty(error(notFound(MSG_REPO_NOT_FOUND, indexStudyRepositoryCommand.getRepositoryCode())))
            .flatMapMany(this::getAllStudies)
            .flatMap(this::getStudyAnalysesDocuments)
            .flatMap(this::batchUpsertFiles)
            .reduce(this::reduceFinalResult);
    }

    @Override
    public void addRule(AddRuleCommand addRuleCommand) {
        throw new IndexerException("not implemented yet");
    }

    @Override
    public void deleteRule(DeleteRuleCommand deleteRuleCommand) {
        throw new IndexerException("not implemented yet");
    }

    @Override
    public List<? extends ExclusionRule> getAllRules() {
        throw new IndexerException("not implemented yet");
    }

    /* **************** *
     * Private Methods  *
     * **************** */
    private Either<IndexerException, StudyAndRepository>
    toStudyAndRepositoryEither(@NonNull IndexStudyCommand indexStudyCommand, StudyRepository filesRepository) {
        return Either.right(
            StudyAndRepository.builder()
                .study(Study.builder().studyId(indexStudyCommand.getStudyId()).build())
                .studyRepository(filesRepository).build()
        );
    }

    private Flux<Either<IndexerException, StudyAndRepository>>
    getAllStudies(StudyRepository studyRepository) {
        return this.studyDAO.getStudies(GetAllStudiesCommand.builder()
            .filesRepositoryBaseUrl(studyRepository.getBaseUrl())
            .build()
        )
        .map(studies -> checkToNotify(studyRepository, studies))
        .map(studyEither -> toStudyAndRepository(studyRepository, studyEither));
    }

    private Either<IndexerException, Study>
    checkToNotify(StudyRepository studyRepository, Either<IndexerException, Study> exceptionStudyEither) {
        if (exceptionStudyEither.isLeft()) {
            notifyFailedToFetchStudies(studyRepository.getCode(), exceptionStudyEither.left().get().getMessage());
        }
        return exceptionStudyEither;
    }

    private Either<IndexerException, StudyAndRepository>
    toStudyAndRepository(StudyRepository studyRepository, Either<IndexerException, Study> studyEither) {
        return studyEither.map(st -> StudyAndRepository.builder()
                .study(st)
                .studyRepository(studyRepository)
                .build()
        );
    }

    private Mono<Either<IndexerException, List<Analysis>>>
    getFilteredAnalyses(StudyRepository repo, String studyId) {
        return fetchAnalyses(repo.getBaseUrl(), studyId)
            .flatMap((analysesEither) ->
                analysesEither.fold(
                    (e) -> Mono.just(Either.left(e)),
                    (analyses) -> this.getExclusionRulesAndFilter(analyses).map(Either::right)
                )
            );
    }

    private Mono<Either<IndexerException, List<Analysis>>>
    fetchAnalyses(String studyRepositoryBaseUrl, String studyId) {
        return this.studyDAO.getStudyAnalyses(GetStudyAnalysesCommand.builder()
            .filesRepositoryBaseUrl(studyRepositoryBaseUrl)
            .studyId(studyId)
            .build()
        ).map((analysesEither) -> {
            analysesEither.left().map(
                e -> {
                    notifyStudyFetchingError(studyId, studyRepositoryBaseUrl, e.getMessage());
                    return e;
                }
            );
            return analysesEither;
        });
    }

    private Mono<List<Analysis>> getExclusionRulesAndFilter(List<Analysis> analyses) {
        return this.exclusionRulesDAO.getExclusionRules()
            .defaultIfEmpty(Map.of())
            .map(ruleMap -> AnalysisAndExclusions.builder()
                .analyses(analyses)
                .exclusionRulesMap(ruleMap)
                .build()
            )
            .map(analysisAndExclusions -> filterExcludedAnalyses(analyses, analysisAndExclusions));
    }

    private List<Analysis> filterExcludedAnalyses(List<Analysis> analyses,
                                                  AnalysisAndExclusions analysisAndExclusions) {
        return analyses.stream()
            .filter(analysis ->
                !ExclusionRulesEvaluator.shouldExcludeAnalysis(analysis, analysisAndExclusions.getExclusionRulesMap()))
            .collect(Collectors.toList());
    }

    private Mono<Either<IndexerException, List<FileCentricDocument>>>
    getStudyAnalysesDocuments(Either<IndexerException, StudyAndRepository> repoAndStudyEither) {
        return repoAndStudyEither.fold(
            (e) -> Mono.just(Either.left(e)),
            (r) -> getFilteredAnalyses(r.getStudyRepository(), r.getStudy().getStudyId())
                .map((analyses) -> getFilesForAnalyses(analyses, r.getStudyRepository()))
        );
    }

    private Either<IndexerException, List<FileCentricDocument>>
    getFilesForAnalyses(Either<IndexerException, List<Analysis>> analyses, StudyRepository studyRepository) {
        return analyses.flatMap(analysesList -> buildAnalysisFileDocuments(studyRepository, analysesList));
    }

    private Either<IndexerException, List<FileCentricDocument>>
    buildAnalysisFileDocuments(StudyRepository repo, List<Analysis> analyses) {
        return analyses.stream()
            .map(analysis -> buildFileDocuments(analysis, repo))
            .reduce((newEither, current) -> newEither.fold(
                (e) -> current.left()
                    .map((newException) -> {
                        e.getFailureData().addFailures(newException.getFailureData());
                        return e;
                    }).toEither(),
                (newList) -> current.right().map(currentCombinedList -> {
                    val combined = new ArrayList<>(currentCombinedList);
                    combined.addAll(newList);
                    return List.copyOf(combined);
                }).toEither()
            ))
            .orElseGet(() -> Either.right(List.of()));
    }

    private Either<IndexerException, List<FileCentricDocument>>
    buildFileDocuments(Analysis analysis, StudyRepository repository) {
        return Try.of(() -> FileCentricDocumentConverter.fromAnalysis(analysis, repository))
            .onFailure((e) -> notifyBuildDocumentFailure(analysis, repository, e))
            .toEither()
            .left()
            .map((t) -> wrapBuildDocumentException(analysis, t))
            .toEither();
    }

    private IndexerException wrapBuildDocumentException(Analysis analysis, Throwable throwable) {
        return wrapWithIndexerException(throwable,
            format("buildFileDocuments failed for analysis : {0}, study: {1}",
                analysis.getAnalysisId(), analysis.getStudy()),
            FailureData.builder()
                .failingIds(Map.of(ANALYSIS_ID, Set.of(analysis.getAnalysisId())))
                .build());
    }

    private void notifyBuildDocumentFailure(Analysis analysis, StudyRepository repository, Throwable e) {
        notifier.notify(
           new IndexerNotification(
               NotificationName.CONVERT_ANALYSIS_TO_FILE_DOCS_FAILED,
               Map.of(
                   ANALYSIS_ID, analysis.getAnalysisId(),
                   STUDY_ID, analysis.getStudy(),
                   REPO_CODE, repository.getCode(),
                   ERR, e.getMessage()
               )
           )
       );
    }

    private Mono<IndexResult> batchUpsertFiles(Either<IndexerException, List<FileCentricDocument>> fileDocsEither) {
        return fileDocsEither.fold(
            (ex) -> {
                val failure = ex != null ? ex.getFailureData() : null;
                return Mono.just(
                    IndexResult.builder()
                        .successful(false)
                        .failureData(failure)
                        .build()
                );
            },
            (fileCentricDocuments) -> this.batchUpsert(fileDocsEither.get())
        );
    }

    private IndexResult reduceFinalResult(IndexResult accumulatedResult, IndexResult newResult) {
        log.trace("In reduceFinalResult, newResult {} ", newResult);
        val both = FailureData.builder().build();

        if (!accumulatedResult.isSuccessful()) {
            both.addFailures(accumulatedResult.getFailureData());
        }

        if (!newResult.isSuccessful()) {
            both.addFailures(newResult.getFailureData());
        }

        return IndexResult.builder()
            .failureData(both)
            .successful(both.getFailingIds().isEmpty())
            .build();
    }

    private Mono<IndexResult> batchUpsert(List<FileCentricDocument> files) {
        return this.fileCentricIndexAdapter.batchUpsertFileRepositories(BatchIndexFilesCommand.builder()
            .files(files)
            .build()
        )
        .map(indexResult -> {
            if (!indexResult.isSuccessful()) {
                notifier.notify(
                    new IndexerNotification(
                        NotificationName.INDEX_REQ_FAILED,
                        Map.of(
                            FAILURE_DATA, indexResult.getFailureData()
                        )
                    )
                );
            }
            return indexResult;
        })
        .doOnSuccess(
            indexResult -> log.trace("finished batchUpsert, list size {}, hashcode {}",
                files.size(), Objects.hashCode(files))
        );
    }

    private void notifyFailedToFetchStudies(String code, String message) {
        val attrs = Map.<String, Object>of(REPO_CODE, code, ERR, message);
        val notification =
            new IndexerNotification(NotificationName.FETCH_REPO_STUDIES_FAILED, attrs);
        notifier.notify(notification);
    }

    private void notifyStudyFetchingError(String studyId, String repoUrl, String excMsg) {
        val attrs = Map.<String, Object>of(STUDY_ID, studyId, REPO_URL, repoUrl, ERR, excMsg);
        val notification =
            new IndexerNotification(NotificationName.STUDY_ANALYSES_FETCH_FAILED, attrs);
        notifier.notify(notification);
    }

    @Getter
    @Builder
    private static class StudyAndRepository {
        private StudyRepository studyRepository;
        private Study study;
    }

    @Getter
    @Builder
    private static class AnalysisAndExclusions {
        private List<Analysis> analyses;
        private Map<Class<?>, List<? extends ExclusionRule>> exclusionRulesMap;
    }

}
