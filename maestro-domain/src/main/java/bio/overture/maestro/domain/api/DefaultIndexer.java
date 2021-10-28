/*
 *  Copyright (c) 2019. Ontario Institute for Cancer Research
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as
 *   published by the Free Software Foundation, either version 3 of the
 *   License, or (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package bio.overture.maestro.domain.api;

import static bio.overture.maestro.domain.api.AnalysisCentricDocumentConverter.fromAnalysis;
import static bio.overture.maestro.domain.api.ExclusionRulesEvaluator.shouldExcludeAnalysis;
import static bio.overture.maestro.domain.utility.Exceptions.wrapWithIndexerException;
import static java.text.MessageFormat.format;

import bio.overture.maestro.domain.api.exception.FailureData;
import bio.overture.maestro.domain.api.exception.IndexerException;
import bio.overture.maestro.domain.api.message.*;
import bio.overture.maestro.domain.entities.indexing.FileCentricDocument;
import bio.overture.maestro.domain.entities.indexing.Repository;
import bio.overture.maestro.domain.entities.indexing.analysis.AnalysisCentricDocument;
import bio.overture.maestro.domain.entities.indexing.rules.ExclusionRule;
import bio.overture.maestro.domain.entities.message.AnalysisMessage;
import bio.overture.maestro.domain.entities.metadata.repository.StudyRepository;
import bio.overture.maestro.domain.entities.metadata.study.Analysis;
import bio.overture.maestro.domain.entities.metadata.study.Study;
import bio.overture.maestro.domain.port.outbound.indexing.AnalysisCentricIndexAdapter;
import bio.overture.maestro.domain.port.outbound.indexing.BatchIndexAnalysisCommand;
import bio.overture.maestro.domain.port.outbound.indexing.BatchIndexFilesCommand;
import bio.overture.maestro.domain.port.outbound.indexing.FileCentricIndexAdapter;
import bio.overture.maestro.domain.port.outbound.indexing.rules.ExclusionRulesDAO;
import bio.overture.maestro.domain.port.outbound.metadata.repository.StudyRepositoryDAO;
import bio.overture.maestro.domain.port.outbound.metadata.study.GetAllStudiesCommand;
import bio.overture.maestro.domain.port.outbound.metadata.study.GetAnalysisCommand;
import bio.overture.maestro.domain.port.outbound.metadata.study.GetStudyAnalysesCommand;
import bio.overture.maestro.domain.port.outbound.metadata.study.StudyDAO;
import bio.overture.maestro.domain.port.outbound.notification.IndexerNotification;
import io.vavr.Tuple2;
import io.vavr.control.Either;
import io.vavr.control.Try;
import java.util.*;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
class DefaultIndexer implements Indexer {

  static final String STUDY_ID = "studyId";
  static final String REPO_CODE = "repoCode";
  static final String ANALYSIS_ID = "analysisId";
  static final String ERR = "err";

  private static final String REPO_URL = "repoUrl";
  private static final String FAILURE_DATA = "failureData";
  private static final String CONFLICTS = "conflicts";
  public static final String ALL = "ALL";
  private final String fileCentricIndexName;
  private final String analysisCentricIndexName;
  private boolean isFileCentricEnabled;
  private boolean isAnalysisCentricEnabled;

  private final FileCentricIndexAdapter fileCentricIndexAdapter;
  private final AnalysisCentricIndexAdapter analysisCentricIndexAdapter;
  private final StudyDAO studyDAO;
  private final StudyRepositoryDAO studyRepositoryDao;
  private final ExclusionRulesDAO exclusionRulesDAO;
  private final Notifier notifier;

  @Inject
  DefaultIndexer(
      FileCentricIndexAdapter fileCentricIndexAdapter,
      AnalysisCentricIndexAdapter analysisCentricIndexAdapter,
      StudyDAO studyDAO,
      StudyRepositoryDAO studyRepositoryDao,
      ExclusionRulesDAO exclusionRulesDAO,
      Notifier notifier,
      IndexProperties indexProperties) {
    this.fileCentricIndexAdapter = fileCentricIndexAdapter;
    this.analysisCentricIndexAdapter = analysisCentricIndexAdapter;
    this.studyDAO = studyDAO;
    this.studyRepositoryDao = studyRepositoryDao;
    this.exclusionRulesDAO = exclusionRulesDAO;
    this.notifier = notifier;
    this.isAnalysisCentricEnabled = indexProperties.isAnalysisCentricEnabled();
    this.isFileCentricEnabled = indexProperties.isFileCentricEnabled();
    this.fileCentricIndexName = indexProperties.fileCentricIndexName();
    this.analysisCentricIndexName = indexProperties.analysisCentricIndexName();
  }

  @Override
  public Flux<IndexResult> indexAnalysis(@NonNull IndexAnalysisCommand command) {
    List<Mono<IndexResult>> monos = new ArrayList<>();
    if (isFileCentricEnabled) {
      monos.add(indexAnalysisToFileCentric(command));
    }
    if (isAnalysisCentricEnabled) {
      monos.add(indexAnalysisToAnalysisCentric(command));
    }
    return Flux.merge(monos);
  }

  @Override
  public Flux<IndexResult> indexAnalysisPayload(@NonNull AnalysisMessage analysis) {
    List<Mono<IndexResult>> monos = new ArrayList<>();
    IndexAnalysisCommand indexAnalysisCommand =
        IndexAnalysisCommand.builder()
            .analysisIdentifier(
                AnalysisIdentifier.builder()
                    .repositoryCode(analysis.getSongServerId())
                    .studyId(analysis.getStudyId())
                    .analysisId(analysis.getAnalysisId())
                    .build())
            .build();
    if (isFileCentricEnabled) {
      monos.add(indexAnalysisPayloadToFileCentric(analysis, indexAnalysisCommand));
    }

    if (isAnalysisCentricEnabled) {
      monos.add(indexAnalysisPayloadToAnalysisCentric(analysis, indexAnalysisCommand));
    }
    return Flux.merge(monos);
  }

  /**
   * Builds file centric document from analysisPayload, indexes the document to file centric index.
   *
   * @param analysisPayload Kafka message body that contains analysis, files, samples.
   * @param command A command that has analysis identifier.
   * @return Index result indicating if documents have been successfully indexed.
   */
  private Mono<IndexResult> indexAnalysisPayloadToFileCentric(
      @NonNull AnalysisMessage analysisPayload, @NonNull IndexAnalysisCommand command) {
    val analysisIdentifier = command.getAnalysisIdentifier();
    return prepareTuple(command)
        .map(
            tuple ->
                buildFileCentricDocuments(
                    tuple.studyRepository, List.of(analysisPayload.getAnalysis())))
        .flatMap(this::batchUpsertFilesAndCollectFailures)
        .onErrorResume(
            IndexerException.class,
            (ex) ->
                Mono.just(this.convertIndexerExceptionToIndexResult(ex, this.fileCentricIndexName)))
        .onErrorResume(
            (e) -> handleIndexAnalysisError(e, analysisIdentifier, this.fileCentricIndexName));
  }

  /**
   * Builds analysis centric document from analysisPayload, indexes the document to analysis centric
   * index.
   *
   * @param analysisPayload Kafka message body that contains analysis, files, samples.
   * @param command A command that has analysis identifier.
   * @return Index result indicating if documents have been successfully indexed.
   */
  private Mono<IndexResult> indexAnalysisPayloadToAnalysisCentric(
      @NonNull AnalysisMessage analysisPayload, @NonNull IndexAnalysisCommand command) {
    val analysisIdentifier = command.getAnalysisIdentifier();
    return prepareTuple(command)
        .map(
            tuple ->
                buildAnalysisCentricDocuments(
                    tuple.studyRepository, List.of(analysisPayload.getAnalysis())))
        .flatMap(this::batchUpsertAnalysesAndCollectFailures)
        .onErrorResume(
            IndexerException.class,
            (ex) ->
                Mono.just(
                    this.convertIndexerExceptionToIndexResult(ex, this.analysisCentricIndexName)))
        .onErrorResume(
            (e) -> handleIndexAnalysisError(e, analysisIdentifier, this.analysisCentricIndexName));
  }

  public Mono<IndexResult> indexAnalysisToAnalysisCentric(
      @NonNull IndexAnalysisCommand indexAnalysisCommand) {
    val analysisIdentifier = indexAnalysisCommand.getAnalysisIdentifier();

    return prepareTuple(indexAnalysisCommand)
        .flatMap(this::getAnalysisCentricDocuments)
        .flatMap(this::batchUpsertAnalysesAndCollectFailures)
        .onErrorResume(
            IndexerException.class,
            (ex) ->
                Mono.just(
                    this.convertIndexerExceptionToIndexResult(ex, this.analysisCentricIndexName)))
        .onErrorResume(
            (e) -> handleIndexAnalysisError(e, analysisIdentifier, this.analysisCentricIndexName));
  }

  public Mono<IndexResult> indexAnalysisToFileCentric(
      @NonNull IndexAnalysisCommand indexAnalysisCommand) {
    val analysisIdentifier = indexAnalysisCommand.getAnalysisIdentifier();

    return prepareTuple(indexAnalysisCommand)
        .flatMap(this::getFileCentricDocuments)
        .flatMap(this::batchUpsertFilesAndCollectFailures)
        // this handles exceptions that were handled already and avoids them getting to the generic
        // handler
        // because we know if we get this exception it was already logged and notified so we don't
        // want that again.
        .onErrorResume(
            IndexerException.class,
            (ex) ->
                Mono.just(this.convertIndexerExceptionToIndexResult(ex, this.fileCentricIndexName)))
        // this handler handles uncaught exceptions
        .onErrorResume(
            (e) -> handleIndexAnalysisError(e, analysisIdentifier, this.fileCentricIndexName));
  }

  @Override
  public Flux<IndexResult> removeAnalysis(@NonNull RemoveAnalysisCommand removeAnalysisCommand) {
    val analysisIdentifier = removeAnalysisCommand.getAnalysisIdentifier();
    List<Mono<IndexResult>> monos = new ArrayList<>();

    if (isFileCentricEnabled) {
      val mono =
          this.fileCentricIndexAdapter
              .removeAnalysisFiles(analysisIdentifier.getAnalysisId())
              .thenReturn(
                  IndexResult.builder()
                      .indexName(this.fileCentricIndexName)
                      .successful(true)
                      .build())
              .onErrorResume(
                  (e) -> handleRemoveAnalysisError(this.fileCentricIndexName, analysisIdentifier));
      monos.add(mono);
    }

    if (isAnalysisCentricEnabled) {
      val mono =
          this.analysisCentricIndexAdapter
              .removeAnalysisDocs(analysisIdentifier.getAnalysisId())
              .thenReturn(
                  IndexResult.builder()
                      .indexName(this.analysisCentricIndexName)
                      .successful(true)
                      .build())
              .onErrorResume(
                  (e) ->
                      handleRemoveAnalysisError(this.analysisCentricIndexName, analysisIdentifier));
      monos.add(mono);
    }
    return Flux.merge(monos);
  }

  @Override
  public Flux<IndexResult> indexStudy(@NonNull IndexStudyCommand command) {
    List<Mono<IndexResult>> monos = new ArrayList<>();

    Mono<Tuple2<List<Analysis>, StudyAndRepository>> mono =
        prepareStudyAndRepo(command)
            .flatMap(
                studyAndRepository ->
                    getFilteredAnalyses(
                            studyAndRepository.getStudyRepository().getUrl(),
                            studyAndRepository.getStudy().getStudyId())
                        .map(analyses -> new Tuple2<>(analyses, studyAndRepository)));

    if (isFileCentricEnabled) {
      monos.add(indexStudyToFileCentric(command, mono));
    }
    if (isAnalysisCentricEnabled) {
      monos.add(indexStudyToAnalysisCentric(command, mono));
    }
    return Flux.merge(monos);
  }

  private Mono<IndexResult> indexStudyToFileCentric(
      @NonNull IndexStudyCommand command,
      @NonNull Mono<Tuple2<List<Analysis>, StudyAndRepository>> tuple2) {
    log.trace("in indexStudyToFileCentric, args: {} ", command);
    return tuple2
        .map(t -> buildFileCentricDocuments(t._2().getStudyRepository(), t._1()))
        .flatMap(this::batchUpsertFilesAndCollectFailures)
        .onErrorResume(
            IndexerException.class,
            (ex) ->
                Mono.just(this.convertIndexerExceptionToIndexResult(ex, this.fileCentricIndexName)))
        .onErrorResume(
            (e) ->
                handleIndexStudyError(
                    e,
                    command.getStudyId(),
                    command.getRepositoryCode(),
                    this.fileCentricIndexName));
  }

  private Mono<IndexResult> indexStudyToAnalysisCentric(
      @NonNull IndexStudyCommand command,
      @NonNull Mono<Tuple2<List<Analysis>, StudyAndRepository>> tuple2) {
    log.trace("in indexStudyToAnalysisCentric, args: {} ", command);
    return tuple2
        .map(t -> buildAnalysisCentricDocuments(t._2().getStudyRepository(), t._1()))
        .flatMap(this::batchUpsertAnalysesAndCollectFailures)
        .onErrorResume(
            IndexerException.class,
            (ex) ->
                Mono.just(
                    this.convertIndexerExceptionToIndexResult(ex, this.analysisCentricIndexName)))
        .onErrorResume(
            (e) ->
                handleIndexStudyError(
                    e,
                    command.getStudyId(),
                    command.getRepositoryCode(),
                    this.analysisCentricIndexName));
  }

  @Override
  public Mono<Map<String, IndexResult>> indexRepository(
      @NonNull IndexStudyRepositoryCommand command) {
    log.trace("in indexRepository, args: {} ", command);
    return tryGetStudyRepository(command.getRepositoryCode())
        .flatMapMany(this::getAllStudies)
        .flatMap(
            studyAndRepository ->
                // I had to put this block inside this flatMap to allow these operations to bubble
                // up
                // their exceptions to this onErrorResume handler without interrupting the main
                // flux,
                // and terminating it with error signals.
                // for example if fetchAnalyses down stream throws error for a studyId the studies
                // flux
                // will continue emitting studies
                this.indexStudy(
                    IndexStudyCommand.builder()
                        .studyId(studyAndRepository.getStudy().getStudyId())
                        .repositoryCode(studyAndRepository.studyRepository.getCode())
                        .build()))
        .onErrorResume(
            IndexerException.class,
            (ex) -> Mono.just(this.convertIndexerExceptionToIndexResult(ex, ALL)))
        .onErrorResume((e) -> handleIndexRepositoryError(e, command.getRepositoryCode()))
        .reduce(
            new HashMap<>(),
            (map, indexResult2) -> {
              val indexName = indexResult2.getIndexName();
              map.put(indexName, reduceIndexResult(map.get(indexName), indexResult2));
              return map;
            });
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
  private Mono<StudyAndRepository> prepareStudyAndRepo(@NonNull IndexStudyCommand command) {
    return tryGetStudyRepository(command.getRepositoryCode())
        .map(filesRepository -> toStudyAndRepositoryTuple(command, filesRepository));
  }

  private Mono<StudyAnalysisRepositoryTuple> prepareTuple(
      @NonNull IndexAnalysisCommand indexAnalysisCommand) {
    val analysisIdentifier = indexAnalysisCommand.getAnalysisIdentifier();
    return tryGetStudyRepository(analysisIdentifier.getRepositoryCode())
        .map(studyRepository -> buildStudyAnalysisRepoTuple(analysisIdentifier, studyRepository));
  }

  private Mono<? extends IndexResult> handleRemoveAnalysisError(
      String indexName, @NonNull AnalysisIdentifier analysisIdentifier) {
    val failureInfo = Map.of(ANALYSIS_ID, Set.of(analysisIdentifier.getAnalysisId()));
    this.notifier.notify(
        new IndexerNotification(NotificationName.FAILED_TO_REMOVE_ANALYSIS, failureInfo));
    return Mono.just(
        IndexResult.builder()
            .indexName(indexName)
            .failureData(FailureData.builder().failingIds(failureInfo).build())
            .build());
  }

  private Mono<StudyRepository> tryGetStudyRepository(@NonNull String repoCode) {
    return this.studyRepositoryDao
        .getFilesRepository(repoCode)
        .onErrorMap((e) -> handleGetStudyRepoError(repoCode, e));
  }

  private Throwable handleGetStudyRepoError(@NonNull String repoCode, Throwable e) {
    val failure = Map.of(REPO_CODE, Set.of(repoCode));
    this.notifier.notify(
        new IndexerNotification(NotificationName.FAILED_TO_FETCH_REPOSITORY, failure));
    return wrapWithIndexerException(
        e, "failed getting repository", FailureData.builder().failingIds(failure).build());
  }

  private Flux<StudyAndRepository> getAllStudies(StudyRepository studyRepository) {
    return this.studyDAO
        .getStudies(
            GetAllStudiesCommand.builder().filesRepositoryBaseUrl(studyRepository.getUrl()).build())
        .onErrorMap((e) -> handleGetStudiesError(studyRepository, e))
        .map(study -> toStudyAndRepository(studyRepository, study));
  }

  @NotNull
  private Throwable handleGetStudiesError(StudyRepository studyRepository, Throwable e) {
    val errMsg = getErrorMessageOrType(e);
    notifyFailedToFetchStudies(studyRepository.getCode(), errMsg);
    return wrapWithIndexerException(
        e,
        "fetch studies failed",
        FailureData.builder()
            .failingIds(Map.of(REPO_CODE, Set.of(studyRepository.getCode())))
            .build());
  }

  private void notifyFailedToFetchStudies(String code, String message) {
    val attrs = Map.<String, Object>of(REPO_CODE, code, ERR, message);
    val notification = new IndexerNotification(NotificationName.FETCH_REPO_STUDIES_FAILED, attrs);
    notifier.notify(notification);
  }

  private StudyAndRepository toStudyAndRepository(
      StudyRepository studyRepository, Study studyEither) {
    return StudyAndRepository.builder().study(studyEither).studyRepository(studyRepository).build();
  }

  private StudyAndRepository toStudyAndRepositoryTuple(
      @NonNull IndexStudyCommand indexStudyCommand, @NonNull StudyRepository filesRepository) {
    return StudyAndRepository.builder()
        .study(Study.builder().studyId(indexStudyCommand.getStudyId()).build())
        .studyRepository(filesRepository)
        .build();
  }

  private Mono<List<Analysis>> getFilteredAnalyses(
      @NonNull String repoBaseUrl, @NonNull String studyId) {
    return fetchAnalyses(repoBaseUrl, studyId).flatMap(this::getExclusionRulesAndFilter);
  }

  private Mono<List<Analysis>> fetchAnalyses(
      @NonNull String studyRepositoryBaseUrl, @NonNull String studyId) {
    val command =
        GetStudyAnalysesCommand.builder()
            .filesRepositoryBaseUrl(studyRepositoryBaseUrl)
            .studyId(studyId)
            .build();

    return this.studyDAO
        .getStudyAnalyses(command)
        .onErrorMap(e -> handleFetchAnalysesError(studyRepositoryBaseUrl, studyId, command, e));
  }

  private Throwable handleFetchAnalysesError(
      String studyRepositoryBaseUrl, String studyId, GetStudyAnalysesCommand command, Throwable e) {
    notifyStudyFetchingError(studyId, studyRepositoryBaseUrl, e.getMessage());
    return wrapWithIndexerException(
        e,
        format("failed fetching studyId analysis, command: {0}, retries exhausted", command),
        FailureData.builder().failingIds(Map.of(STUDY_ID, Set.of(studyId))).build());
  }

  private void notifyStudyFetchingError(String studyId, String repoUrl, String excMsg) {
    val attrs = Map.<String, Object>of(STUDY_ID, studyId, REPO_URL, repoUrl, ERR, excMsg);
    val notification = new IndexerNotification(NotificationName.STUDY_ANALYSES_FETCH_FAILED, attrs);
    notifier.notify(notification);
  }

  private StudyAnalysisRepositoryTuple buildStudyAnalysisRepoTuple(
      @NonNull AnalysisIdentifier indexAnalysisCommand, StudyRepository filesRepository) {
    return StudyAnalysisRepositoryTuple.builder()
        .analysisId(indexAnalysisCommand.getAnalysisId())
        .study(Study.builder().studyId(indexAnalysisCommand.getStudyId()).build())
        .studyRepository(filesRepository)
        .build();
  }

  private Mono<List<Analysis>> getAnalysisFromStudyRepository(StudyAnalysisRepositoryTuple tuple) {
    return tryFetchAnalysis(tuple).flatMap(this::getExclusionRulesAndFilter);
  }

  private Mono<Tuple2<FailureData, List<FileCentricDocument>>> getFileCentricDocuments(
      StudyAnalysisRepositoryTuple tuple) {
    return getAnalysisFromStudyRepository(tuple)
        .map((analyses) -> buildFileCentricDocuments(tuple.studyRepository, analyses));
  }

  private Mono<Tuple2<FailureData, List<AnalysisCentricDocument>>> getAnalysisCentricDocuments(
      StudyAnalysisRepositoryTuple tuple) {
    return getAnalysisFromStudyRepository(tuple)
        .map((analyses -> buildAnalysisCentricDocuments(tuple.studyRepository, analyses)));
  }

  private Mono<List<Analysis>> tryFetchAnalysis(StudyAnalysisRepositoryTuple tuple) {
    return this.studyDAO
        .getAnalysis(
            GetAnalysisCommand.builder()
                .analysisId(tuple.getAnalysisId())
                .filesRepositoryBaseUrl(tuple.getStudyRepository().getUrl())
                .studyId(tuple.getStudy().getStudyId())
                .build())
        .map(List::of)
        .onErrorMap((e) -> handleFetchAnalysisError(tuple, e));
  }

  private IndexResult convertIndexerExceptionToIndexResult(IndexerException e, String indexName) {
    return IndexResult.builder()
        .indexName(indexName)
        .failureData(e.getFailureData())
        .successful(false)
        .build();
  }

  private Mono<IndexResult> handleIndexStudyError(
      Throwable e, String studyId, String repoCode, String indexName) {
    val context =
        Map.of(
            STUDY_ID, studyId,
            REPO_CODE, repoCode,
            ERR, getErrorMessageOrType(e));
    val failingId = Map.of(STUDY_ID, Set.of(studyId));
    return notifyAndReturnFallback(failingId, context, indexName);
  }

  private Mono<IndexResult> handleIndexAnalysisError(
      Throwable e, @NonNull AnalysisIdentifier indexAnalysisCommand, @NonNull String indexName) {
    log.error("unhandled exception while indexing analysis", e);
    val failureContext =
        Map.of(
            ANALYSIS_ID, indexAnalysisCommand.getAnalysisId(),
            STUDY_ID, indexAnalysisCommand.getStudyId(),
            REPO_CODE, indexAnalysisCommand.getRepositoryCode(),
            ERR, getErrorMessageOrType(e));
    val failedAnalysisId = Map.of(ANALYSIS_ID, Set.of(indexAnalysisCommand.getAnalysisId()));
    return notifyAndReturnFallback(failedAnalysisId, failureContext, indexName);
  }

  @NotNull
  private Mono<IndexResult> notifyAndReturnFallback(
      Map<String, Set<String>> failingIds,
      Map<String, ? extends Object> contextInfo,
      String indexName) {
    this.notifier.notify(new IndexerNotification(NotificationName.UNHANDLED_ERROR, contextInfo));
    return Mono.just(
        IndexResult.builder()
            .indexName(indexName)
            .failureData(FailureData.builder().failingIds(failingIds).build())
            .successful(false)
            .build());
  }

  private Mono<List<Analysis>> getExclusionRulesAndFilter(List<Analysis> analyses) {
    return this.exclusionRulesDAO
        .getExclusionRules()
        .defaultIfEmpty(Map.of())
        .map(
            ruleMap ->
                AnalysisAndExclusions.builder()
                    .analyses(analyses)
                    .exclusionRulesMap(ruleMap)
                    .build())
        .map(analysisAndExclusions -> filterExcludedAnalyses(analyses, analysisAndExclusions))
        .onErrorMap((e) -> handleExclusionStepError(analyses, e));
  }

  private Throwable handleExclusionStepError(List<Analysis> analyses, Throwable e) {
    val failureInfo =
        Map.of(
            ANALYSIS_ID,
            analyses.stream().map(Analysis::getAnalysisId).collect(Collectors.toUnmodifiableSet()));
    notifier.notify(
        new IndexerNotification(NotificationName.FAILED_TO_FETCH_ANALYSIS, failureInfo));
    return wrapWithIndexerException(
        e, "failed filtering analysis", FailureData.builder().failingIds(failureInfo).build());
  }

  private Tuple2<FailureData, List<FileCentricDocument>> buildFileCentricDocuments(
      StudyRepository repo, List<Analysis> analyses) {

    return analyses.stream()
        .map(analysis -> buildFileDocuments(analysis, repo))
        .map(
            newEither ->
                newEither.fold(
                    (left) -> new Tuple2<>(left.getFailureData(), List.<FileCentricDocument>of()),
                    (right) -> new Tuple2<>(FailureData.builder().build(), right)))
        .reduce(
            (accumulated, current) -> {
              accumulated._1().addFailures(current._1());
              val combined = new ArrayList<>(accumulated._2());
              combined.addAll(current._2());
              return new Tuple2<>(accumulated._1(), Collections.unmodifiableList(combined));
            })
        .orElseGet(() -> new Tuple2<>(FailureData.builder().build(), List.of()));
  }

  private Tuple2<FailureData, List<AnalysisCentricDocument>> buildAnalysisCentricDocuments(
      StudyRepository repo, List<Analysis> analyses) {
    return analyses.stream()
        .map(analysis -> buildAnalysisDocuments(analysis, repo))
        .map(
            newEither ->
                newEither.fold(
                    (left) ->
                        new Tuple2<>(left.getFailureData(), List.<AnalysisCentricDocument>of()),
                    (right) -> new Tuple2<>(FailureData.builder().build(), right)))
        .reduce(
            (accumulated, current) -> {
              accumulated._1().addFailures(current._1());
              val combined = new ArrayList<>(accumulated._2());
              combined.addAll(current._2());
              return new Tuple2<>(accumulated._1(), Collections.unmodifiableList(combined));
            })
        .orElseGet(() -> new Tuple2<>(FailureData.builder().build(), List.of()));
  }

  private Mono<IndexResult> batchUpsert(List<FileCentricDocument> files) {
    return getAlreadyIndexed(files)
        .map(storedFilesList -> findConflicts(files, storedFilesList))
        .flatMap(
            conflictsCheckResult -> {
              handleConflicts(conflictsCheckResult);
              return Mono.just(conflictsCheckResult);
            })
        .map(
            conflictsCheckResult ->
                removeConflictingFromInputFilesList(files, conflictsCheckResult))
        .flatMap(this::callBatchUpsert)
        .doOnNext(this::notifyIndexRequestFailures)
        .onErrorResume(
            (ex) -> ex instanceof IndexerException,
            (ex) ->
                Mono.just(
                    IndexResult.builder()
                        .indexName(this.fileCentricIndexName)
                        .successful(false)
                        .failureData(((IndexerException) ex).getFailureData())
                        .build()))
        .doOnSuccess(
            indexResult ->
                log.trace(
                    "finished batchUpsert, list size {}, hashcode {}",
                    files.size(),
                    Objects.hashCode(files)));
  }

  private Mono<IndexResult> batchUpsertAnalysis(List<AnalysisCentricDocument> analyses) {
    return getIndexedAnalyses(analyses)
        .map(storedAnalyses -> findAnalysisConflicts(analyses, storedAnalyses))
        .flatMap(
            conflictsCheckResult -> {
              handleAnalysisConflicts(conflictsCheckResult);
              return Mono.just(conflictsCheckResult);
            })
        .map(
            analysesConflictsResult -> removeConflictingAnalysis(analyses, analysesConflictsResult))
        .flatMap(this::callBatchUpsertAnalysis)
        .doOnNext(this::notifyIndexRequestFailures)
        .onErrorResume(
            (ex) -> ex instanceof IndexerException,
            (ex) ->
                Mono.just(
                    IndexResult.builder()
                        .successful(false)
                        .failureData(((IndexerException) ex).getFailureData())
                        .build()))
        .doOnSuccess(
            indexResult ->
                log.trace(
                    "finished batch upsert analysis, list size {}, hashcode {}",
                    analyses.size(),
                    Objects.hashCode(analyses)));
  }

  private Mono<Map<String, AnalysisCentricDocument>> getIndexedAnalyses(
      List<AnalysisCentricDocument> analyses) {
    return analysisCentricIndexAdapter
        .fetchByIds(
            analyses.stream()
                .map(AnalysisCentricDocument::getAnalysisId)
                .collect(Collectors.toList()))
        .map(
            (fetchResult) -> {
              val idToFileMap = new HashMap<String, AnalysisCentricDocument>();
              fetchResult.forEach(item -> idToFileMap.put(item.getAnalysisId(), item));
              return Collections.unmodifiableMap(idToFileMap);
            });
  }

  private List<Analysis> filterExcludedAnalyses(
      List<Analysis> analyses, AnalysisAndExclusions analysisAndExclusions) {
    return analyses.stream()
        .filter(
            analysis ->
                !shouldExcludeAnalysis(analysis, analysisAndExclusions.getExclusionRulesMap()))
        .collect(Collectors.toList());
  }

  private Either<IndexerException, List<FileCentricDocument>> buildFileDocuments(
      Analysis analysis, StudyRepository repository) {

    return Try.of(() -> FileCentricDocumentConverter.fromAnalysis(analysis, repository))
        .onFailure(
            (e) ->
                notifyBuildDocumentFailure(
                    NotificationName.CONVERT_ANALYSIS_TO_FILE_DOCS_FAILED, analysis, repository, e))
        .toEither()
        .left()
        .map((t) -> wrapBuildDocumentException(analysis, t))
        .toEither();
  }

  private Either<IndexerException, List<AnalysisCentricDocument>> buildAnalysisDocuments(
      Analysis analysis, StudyRepository repository) {

    return Try.of(() -> fromAnalysis(analysis, repository))
        .onFailure(
            (e) ->
                notifyBuildDocumentFailure(
                    NotificationName.CONVERT_ANALYSIS_TO_ANALYSIS_DOCS_FAILED,
                    analysis,
                    repository,
                    e))
        .toEither()
        .left()
        .map((t) -> wrapBuildDocumentException(analysis, t))
        .toEither();
  }

  // if there is already a record in another song
  private Mono<Map<String, FileCentricDocument>> getAlreadyIndexed(
      List<FileCentricDocument> files) {
    return fileCentricIndexAdapter
        .fetchByIds(
            files.stream().map(FileCentricDocument::getObjectId).collect(Collectors.toList()))
        .map(
            (fetchResult) -> {
              // we convert this list to a hash map to optimize performance for large lists when we
              // lookup files by Ids
              val idToFileMap = new HashMap<String, FileCentricDocument>();
              fetchResult.forEach(item -> idToFileMap.put(item.getObjectId(), item));
              return Collections.unmodifiableMap(idToFileMap);
            });
  }

  private ConflictsCheckResult findConflicts(
      List<FileCentricDocument> filesToIndex, Map<String, FileCentricDocument> storedFiles) {
    val conflictingPairs =
        filesToIndex.stream()
            .map(fileToIndex -> findIfAnyStoredFileConflicts(storedFiles, fileToIndex))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    return ConflictsCheckResult.builder().conflictingFiles(conflictingPairs).build();
  }

  private AnalysesConflictsResult findAnalysisConflicts(
      List<AnalysisCentricDocument> toIndex, Map<String, AnalysisCentricDocument> stored) {
    val conflictingPairs =
        toIndex.stream()
            .map(analysisToIndex -> findIfStoredAnalysisConflicts(stored, analysisToIndex))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    return AnalysesConflictsResult.builder().conflictingAnalyses(conflictingPairs).build();
  }

  private Tuple2<AnalysisCentricDocument, AnalysisCentricDocument> findIfStoredAnalysisConflicts(
      Map<String, AnalysisCentricDocument> storedAnalyses, AnalysisCentricDocument toIndex) {
    if (storedAnalyses.containsKey(toIndex.getAnalysisId())) {
      val existingAnalysis = storedAnalyses.get(toIndex.getAnalysisId());
      if (toIndex.isValidReplica(existingAnalysis)) {
        return null;
      }
      // the analysis to be indexed is a conflict to existing analysis
      return new Tuple2<>(toIndex, storedAnalyses.get(toIndex.getAnalysisId()));
    }
    // there is no conflict when the analysis to be indexed does not exist
    else return null;
  }

  private Throwable handleFetchAnalysisError(StudyAnalysisRepositoryTuple tuple, Throwable e) {
    log.error("failed to fetch analysis", e);
    val notificationInfo =
        Map.of(
            ANALYSIS_ID, tuple.getAnalysisId(),
            REPO_CODE, tuple.getStudyRepository().getCode(),
            STUDY_ID, tuple.getStudy().getStudyId(),
            ERR, e.getMessage());
    val failureInfo = Map.of(ANALYSIS_ID, Set.of(tuple.getAnalysisId()));
    notifier.notify(
        new IndexerNotification(NotificationName.FAILED_TO_FETCH_ANALYSIS, notificationInfo));
    return wrapWithIndexerException(
        e, "failed getting analysis", FailureData.builder().failingIds(failureInfo).build());
  }

  private void handleConflicts(ConflictsCheckResult conflictingFiles) {
    if (conflictingFiles == null || conflictingFiles.getConflictingFiles().isEmpty()) return;
    this.notifyConflicts(conflictingFiles);
  }

  private void handleAnalysisConflicts(AnalysesConflictsResult conflictingAnalyses) {
    if (conflictingAnalyses == null || conflictingAnalyses.getConflictingAnalyses().isEmpty())
      return;
    this.notifyAnalysesConflicts(conflictingAnalyses);
  }

  private List<FileCentricDocument> removeConflictingFromInputFilesList(
      List<FileCentricDocument> files, ConflictsCheckResult conflictsCheckResult) {
    return files.stream()
        .filter(
            fileCentricDocument -> !isInConflictsList(conflictsCheckResult, fileCentricDocument))
        .collect(Collectors.toUnmodifiableList());
  }

  private List<AnalysisCentricDocument> removeConflictingAnalysis(
      List<AnalysisCentricDocument> analyses, AnalysesConflictsResult result) {
    return analyses.stream()
        .filter(
            analysisCentricDocument -> !isInAnalysisConflictsList(result, analysisCentricDocument))
        .collect(Collectors.toUnmodifiableList());
  }

  private Mono<IndexResult> callBatchUpsert(List<FileCentricDocument> conflictFreeFilesList) {
    return this.fileCentricIndexAdapter.batchUpsertFileRepositories(
        BatchIndexFilesCommand.builder().files(conflictFreeFilesList).build());
  }

  private Mono<IndexResult> callBatchUpsertAnalysis(List<AnalysisCentricDocument> analyses) {
    return this.analysisCentricIndexAdapter.batchUpsertAnalysisRepositories(
        BatchIndexAnalysisCommand.builder().analyses(analyses).build());
  }

  private void notifyIndexRequestFailures(IndexResult indexResult) {
    if (!indexResult.isSuccessful()) {
      notifier.notify(
          new IndexerNotification(
              NotificationName.INDEX_REQ_FAILED,
              Map.of(FAILURE_DATA, indexResult.getFailureData())));
    }
  }

  private void notifyBuildDocumentFailure(
      NotificationName notificationName,
      Analysis analysis,
      StudyRepository repository,
      Throwable e) {
    notifier.notify(
        new IndexerNotification(
            notificationName,
            Map.of(
                ANALYSIS_ID, analysis.getAnalysisId(),
                STUDY_ID, analysis.getStudyId(),
                REPO_CODE, repository.getCode(),
                ERR, e.getMessage())));
  }

  private IndexerException wrapBuildDocumentException(Analysis analysis, Throwable throwable) {
    return wrapWithIndexerException(
        throwable,
        format(
            "buildFileDocuments failed for analysis : {0}, studyId: {1}",
            analysis.getAnalysisId(), analysis.getStudyId()),
        FailureData.builder()
            .failingIds(Map.of(ANALYSIS_ID, Set.of(analysis.getAnalysisId())))
            .build());
  }

  private Tuple2<FileCentricDocument, FileCentricDocument> findIfAnyStoredFileConflicts(
      Map<String, FileCentricDocument> storedFiles, FileCentricDocument fileToIndex) {
    if (storedFiles.containsKey(fileToIndex.getObjectId())) {
      val storedFile = storedFiles.get(fileToIndex.getObjectId());
      if (fileToIndex.isValidReplica(storedFile)) {
        return null;
      }
      return new Tuple2<>(fileToIndex, storedFiles.get(fileToIndex.getObjectId()));
    } else {
      return null;
    }
  }

  private void notifyConflicts(ConflictsCheckResult conflictsCheckResult) {
    val conflictingFileList =
        conflictsCheckResult.getConflictingFiles().stream()
            .map(tuple -> tuple.apply(this::toFileConflict))
            .collect(Collectors.toUnmodifiableList());
    val notification =
        new IndexerNotification(
            NotificationName.INDEX_FILE_CONFLICT, Map.of(CONFLICTS, conflictingFileList));
    this.notifier.notify(notification);
  }

  private void notifyAnalysesConflicts(AnalysesConflictsResult analysesConflictsResult) {
    val conflictList =
        analysesConflictsResult.getConflictingAnalyses().stream()
            .map(tuple -> tuple.apply(this::toAnalysisConflict))
            .collect(Collectors.toUnmodifiableList());
    val notification =
        new IndexerNotification(
            NotificationName.ANALYSIS_CONFLICT, Map.of(CONFLICTS, conflictList));
    this.notifier.notify(notification);
  }

  private boolean isInConflictsList(
      ConflictsCheckResult conflictsCheckResult, FileCentricDocument fileCentricDocument) {
    return conflictsCheckResult.getConflictingFiles().stream()
        .map(Tuple2::_1)
        .anyMatch(
            conflictingFile ->
                conflictingFile.getObjectId().equals(fileCentricDocument.getObjectId()));
  }

  private boolean isInAnalysisConflictsList(
      AnalysesConflictsResult analysesConflictsResult,
      AnalysisCentricDocument analysisCentricDocument) {
    return analysesConflictsResult.getConflictingAnalyses().stream()
        .map(Tuple2::_1)
        .anyMatch(
            conflictingAnalysis ->
                conflictingAnalysis
                    .getAnalysisId()
                    .equals(analysisCentricDocument.getAnalysisId()));
  }

  private FileConflict toFileConflict(FileCentricDocument f1, FileCentricDocument f2) {
    return FileConflict.builder()
        .newFile(
            ConflictingFile.builder()
                .objectId(f1.getObjectId())
                .analysisId(f1.getAnalysis().getAnalysisId())
                .studyId(f1.getStudyId())
                .repoCode(
                    f1.getRepositories().stream()
                        .map(Repository::getCode)
                        .collect(Collectors.toUnmodifiableSet()))
                .build())
        .indexedFile(
            ConflictingFile.builder()
                .objectId(f2.getObjectId())
                .analysisId(f2.getAnalysis().getAnalysisId())
                .studyId(f2.getStudyId())
                .repoCode(
                    f2.getRepositories().stream()
                        .map(Repository::getCode)
                        .collect(Collectors.toUnmodifiableSet()))
                .build())
        .build();
  }

  private AnalysisConflict toAnalysisConflict(
      AnalysisCentricDocument a1, AnalysisCentricDocument a2) {
    return AnalysisConflict.builder()
        .newAnalysis(
            ConflictingAnalysis.builder()
                .analysisId(a1.getAnalysisId())
                .studyId(a1.getStudyId())
                .repoCode(
                    a1.getRepositories().stream()
                        .map(Repository::getCode)
                        .collect(Collectors.toUnmodifiableSet()))
                .build())
        .indexedAnalysis(
            ConflictingAnalysis.builder()
                .analysisId(a2.getAnalysisId())
                .studyId(a2.getStudyId())
                .repoCode(
                    a2.getRepositories().stream()
                        .map(Repository::getCode)
                        .collect(Collectors.toSet()))
                .build())
        .build();
  }

  private Mono<IndexResult> batchUpsertFilesAndCollectFailures(
      Tuple2<FailureData, List<FileCentricDocument>> tuple) {
    return this.batchUpsert(tuple._2())
        .map(
            upsertResult ->
                reduceIndexResult(
                    IndexResult.builder().failureData(tuple._1()).build(), upsertResult));
  }

  private Mono<IndexResult> batchUpsertAnalysesAndCollectFailures(
      Tuple2<FailureData, List<AnalysisCentricDocument>> tuple) {
    return this.batchUpsertAnalysis(tuple._2())
        .map(
            upsertResult ->
                reduceIndexResult(
                    IndexResult.builder().failureData(tuple._1()).build(), upsertResult));
  }

  private Mono<? extends IndexResult> handleIndexRepositoryError(
      Throwable e, String repositoryCode) {
    val failingId = Map.of(REPO_CODE, Set.of(repositoryCode));
    val contextInfo = Map.of(REPO_CODE, repositoryCode, ERR, e.getMessage());
    return this.notifyAndReturnFallback(failingId, contextInfo, ALL);
  }

  private IndexResult reduceIndexResult(IndexResult accumulatedResult, IndexResult newResult) {
    log.trace("In reduceIndexResult, newResult {} ", newResult);
    val both = FailureData.builder().build();
    if (accumulatedResult != null && !accumulatedResult.isSuccessful()) {
      both.addFailures(accumulatedResult.getFailureData());
    }
    if (!newResult.isSuccessful()) {
      both.addFailures(newResult.getFailureData());
    }
    return IndexResult.builder()
        .indexName(newResult.getIndexName())
        .failureData(both)
        .successful(both.getFailingIds().isEmpty())
        .build();
  }

  private String getErrorMessageOrType(Throwable e) {
    return e.getMessage() == null ? e.getClass().getName() : e.getMessage();
  }

  @Getter
  @Builder
  @ToString
  @EqualsAndHashCode
  static class FileConflict {
    private ConflictingFile newFile;
    private ConflictingFile indexedFile;
  }

  @Getter
  @Builder
  @ToString
  @EqualsAndHashCode
  static class AnalysisConflict {
    private ConflictingAnalysis newAnalysis;
    private ConflictingAnalysis indexedAnalysis;
  }

  @Getter
  @Builder
  @ToString
  @EqualsAndHashCode
  static class ConflictingFile {
    private String objectId;
    private String analysisId;
    private String studyId;
    private Set<String> repoCode;
  }

  @Getter
  @Builder
  @ToString
  @EqualsAndHashCode
  static class ConflictingAnalysis {
    private String analysisId;
    private String studyId;
    private Set<String> repoCode;
  }

  @Getter
  @Builder
  @ToString
  @EqualsAndHashCode
  private static class ConflictsCheckResult {
    private List<Tuple2<FileCentricDocument, FileCentricDocument>> conflictingFiles;
  }

  @Getter
  @Builder
  @ToString
  @EqualsAndHashCode
  private static class AnalysesConflictsResult {
    private List<Tuple2<AnalysisCentricDocument, AnalysisCentricDocument>> conflictingAnalyses;
  }

  @Getter
  @Builder
  @ToString
  @EqualsAndHashCode
  private static class StudyAnalysisRepositoryTuple {
    private StudyRepository studyRepository;
    private Study study;
    private String analysisId;
  }

  @Getter
  @Builder
  @ToString
  @EqualsAndHashCode
  public static class StudyAndRepository {
    private StudyRepository studyRepository;
    private Study study;
  }

  @Getter
  @Builder
  @ToString
  @EqualsAndHashCode
  private static class AnalysisAndExclusions {
    private List<Analysis> analyses;
    private Map<Class<?>, List<? extends ExclusionRule>> exclusionRulesMap;
  }
}
