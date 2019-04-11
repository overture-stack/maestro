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
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static bio.overture.maestro.domain.utility.Exceptions.notFound;
import static reactor.core.publisher.Mono.error;

@Slf4j
class DefaultIndexer implements Indexer {

    private static final String MSG_REPO_NOT_FOUND = "Repository {0} not found";
    private static final String MSG_EMPTY_REPOSITORY = "Empty repository {0}";
    private static final String STUDY_ID = "studyId";
    private static final String REPO_URL = "repoUrl";
    private static final String ERR = "err";
    private static final String REPO_CODE = "repoCode";
    private static final String ANALYSIS_ID = "analysisId";
    public static final String FAILURE_DATA = "failure_data";
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
            .flatMap(filesRepository -> getAnalysesDocuments(filesRepository, indexStudyCommand.getStudyId()))
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

    /* *****************
     * Private Methods *
     * *****************/

    private Mono<Either<IndexerException, List<FileCentricDocument>>>
        getAnalysesDocuments(StudyRepository filesRepository, String studyId) {

        return getFilteredAnalyses(filesRepository, studyId)
            .map((analyses) -> getFilesForAnalyses(analyses, filesRepository));
    }

    private Flux<Either<IndexerException, StudyAndRepository>> getAllStudies(StudyRepository studyRepository) {
        return this.studyDAO.getStudies(GetAllStudiesCommand.builder()
            .filesRepositoryBaseUrl(studyRepository.getBaseUrl())
            .build()
        )
        .map(studies -> checkToNotify(studyRepository, studies))
        .map(studyEither -> toStudyAndRepository(studyRepository, studyEither));
    }

    @NotNull
    private Either<IndexerException, Study> checkToNotify(StudyRepository studyRepository,
                                                          Either<IndexerException, Study> exceptionStudyEither) {
        if (exceptionStudyEither.isLeft()) {
            notifyFailedToFetchStudies(studyRepository.getCode(), exceptionStudyEither.left().get().getMessage());
        }
        return exceptionStudyEither;
    }

    private Either<IndexerException, StudyAndRepository> toStudyAndRepository(StudyRepository studyRepository,
                                                                              Either<IndexerException, Study> studyEither) {
        if (studyEither.isLeft()) {
            return Either.left(studyEither.getLeft());
        }
        return studyEither.right().map(st -> StudyAndRepository.builder()
                .study(st)
                .studyRepository(studyRepository)
                .build()
        ).toEither();
    }

    private Mono<Either<IndexerException, List<Analysis>>> getFilteredAnalyses(StudyRepository repo, String studyId) {

        return fetchAnalyses(repo.getBaseUrl(), studyId)
            .flatMap((analysesEither) -> {
                if (analysesEither.isLeft()) {
                    return Mono.just(analysesEither);
                }
                return this.getExclusionRulesAndFilter(analysesEither.right().get())
                        .map(Either::right);
            });
    }

    private Mono<Either<IndexerException, List<Analysis>>> fetchAnalyses(String studyRepositoryBaseUrl, String studyId) {
        return this.studyDAO.getStudyAnalyses(GetStudyAnalysesCommand.builder()
            .filesRepositoryBaseUrl(studyRepositoryBaseUrl)
            .studyId(studyId)
            .build()
        )
        .map((analysesEither) -> {
            if (analysesEither.isLeft()) {
                notifyStudyFetchingError(studyId, studyRepositoryBaseUrl, analysesEither.left().get().getMessage());
            }
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

    private List<FileCentricDocument> buildAnalysisFileDocuments(StudyRepository repo, List<Analysis> analyses) {
        return analyses.stream()
            .map(analysis -> buildFileDocuments(analysis, repo))
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    private Mono<Either<IndexerException, List<FileCentricDocument>>>
        getStudyAnalysesDocuments(Either<IndexerException, StudyAndRepository> repoAndStudyEither) {

        if (repoAndStudyEither.isLeft()) {
            return Mono.just(Either.left(repoAndStudyEither.getLeft()));
        }

        val sr = repoAndStudyEither.right().get();
        return getFilteredAnalyses(sr.getStudyRepository(), sr.getStudy().getStudyId())
            .map((analyses) -> getFilesForAnalyses(analyses, sr.getStudyRepository()));
    }

    @NotNull
    private Either<IndexerException, List<FileCentricDocument>> getFilesForAnalyses(Either<IndexerException,
                                                                                    List<Analysis>> analyses,
                                                                                    StudyRepository studyRepository) {
        if (analyses.isLeft()) {
            return Either.left(analyses.getLeft());
        }
        return Either.right(
            buildAnalysisFileDocuments(studyRepository, analyses.right().get())
        );
    }

    private List<FileCentricDocument> buildFileDocuments(Analysis analysis, StudyRepository repository) {
        try {
            return FileCentricDocumentConverter.fromAnalysis(analysis, repository);
        } catch (Exception e) {
            notifier.notify(
                new IndexerNotification(
                    NotificationName.CONVERT_ANALYSIS_TO_FILE_DOCS_FAILED,
                    Map.of(
                        ANALYSIS_ID, analysis.getAnalysisId(),
                        REPO_CODE, repository.getCode(),
                        ERR, e.getMessage()
                    )
                )
            );
            return List.of();
        }
    }

    private Mono<IndexResult> batchUpsertFiles(Either<IndexerException, List<FileCentricDocument>> fileDocsEither) {
        if (fileDocsEither.isLeft()) {
            val ex = fileDocsEither.left().get();
            val failure = ex != null ? ex.getFailureData() : null;
            log.debug("in errorResume, fails {} ", failure);
            return Mono.just(
                    IndexResult.builder()
                    .successful(false)
                    .failureData(failure)
                    .build()
                );
        }
        return this.batchUpsert(fileDocsEither.get());
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
        IndexerNotification notification =
            new IndexerNotification(NotificationName.FETCH_REPO_STUDIES_FAILED, attrs);
        notifier.notify(notification);
    }

    private void notifyStudyFetchingError(String studyId, String repoUrl, String excMsg) {
        val attrs = Map.<String, Object>of(STUDY_ID, studyId, REPO_URL, repoUrl, ERR, excMsg);
        IndexerNotification notification =
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
