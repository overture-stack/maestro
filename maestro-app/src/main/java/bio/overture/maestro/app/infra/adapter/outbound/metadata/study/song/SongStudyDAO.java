package bio.overture.maestro.app.infra.adapter.outbound.metadata.study.song;

import bio.overture.maestro.app.infra.config.properties.ApplicationProperties;
import bio.overture.maestro.domain.api.exception.FailureData;
import bio.overture.maestro.domain.api.exception.IndexerException;
import bio.overture.maestro.domain.api.exception.NotFoundException;
import bio.overture.maestro.domain.entities.metadata.study.Analysis;
import bio.overture.maestro.domain.entities.metadata.study.Study;
import bio.overture.maestro.domain.port.outbound.metadata.study.GetAllStudiesCommand;
import bio.overture.maestro.domain.port.outbound.metadata.study.GetAnalysisCommand;
import bio.overture.maestro.domain.port.outbound.metadata.study.GetStudyAnalysesCommand;
import bio.overture.maestro.domain.port.outbound.metadata.study.StudyDAO;
import io.vavr.control.Either;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.retry.Retry;

import javax.inject.Inject;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static bio.overture.maestro.domain.utility.Exceptions.notFound;
import static bio.overture.maestro.domain.utility.Exceptions.wrapWithIndexerException;
import static java.text.MessageFormat.format;
import static reactor.core.publisher.Mono.error;

@Slf4j
class SongStudyDAO implements StudyDAO {

    static final String STUDY_ID = "studyId";
    private static final String STUDY_ANALYSES_URL_TEMPLATE = "{0}/studies/{1}/analysis?analysisStates={2}";
    private static final String STUDY_ANALYSIS_URL_TEMPLATE = "{0}/studies/{1}/analysis/{2}";
    private static final String STUDIES_URL_TEMPLATE = "{0}/studies/all";
    private static final String MSG_STUDY_DOES_NOT_EXIST = "study {0} doesn't exist  in the specified repository";
    private static final String MSG_ANALYSIS_DOES_NOT_EXIST =
        "analysis {0} doesn't exist for study {1}, repository {2} (or not in a matching state)";
    private static final int FALLBACK_SONG_TIMEOUT = 60;
    private static final int FALLBACK_SONG_ANALYSIS_TIMEOUT = 5;
    private static final int FALLBACK_SONG_MAX_RETRY = 0;
    private static final String REPOSITORY = "repository";
    private final WebClient webClient;
    private final int songMaxRetries;
    private final int minBackoffSec = 1;
    private final int maxBackoffSec = 5;
    private final String indexableStudyStatuses;
    private final List<String> indexableStudyStatusesList;
    /**
     * must be bigger than 0 or all calls will fail
     */
    private final int studyCallTimeoutSeconds;
    private final int analysisCallTimeoutSeconds;
    @Inject
    public SongStudyDAO(@NonNull WebClient webClient, @NonNull ApplicationProperties applicationProperties) {
        this.webClient = webClient;
        this.indexableStudyStatuses = applicationProperties.indexableStudyStatuses();
        this.indexableStudyStatusesList = List.of(indexableStudyStatuses.split(","));
        this.songMaxRetries = applicationProperties.songMaxRetries() >= 0 ? applicationProperties.songMaxRetries()
            : FALLBACK_SONG_MAX_RETRY;
        this.studyCallTimeoutSeconds = applicationProperties.songStudyCallTimeoutSeconds() > 0 ? applicationProperties.songStudyCallTimeoutSeconds()
            : FALLBACK_SONG_TIMEOUT;
        this.analysisCallTimeoutSeconds = applicationProperties.songAnalysisCallTimeoutSeconds() > 0 ?
            applicationProperties.songAnalysisCallTimeoutSeconds(): FALLBACK_SONG_ANALYSIS_TIMEOUT;
    }

    @Override
    @NonNull
    public Mono<List<Analysis>> getStudyAnalyses(GetStudyAnalysesCommand getStudyAnalysesCommand) {
        log.trace("in getStudyAnalyses, args: {} ", getStudyAnalysesCommand);
        val repoBaseUrl = getStudyAnalysesCommand.getFilesRepositoryBaseUrl();
        val studyId = getStudyAnalysesCommand.getStudyId();
        val analysisListType = new ParameterizedTypeReference<List<Analysis>>(){};
        val retryConfig = Retry.allBut(NotFoundException.class)
            .retryMax(this.songMaxRetries)
            .doOnRetry(retryCtx -> log.debug("retrying  {}", getStudyAnalysesCommand))
            .exponentialBackoff(Duration.ofSeconds(minBackoffSec), Duration.ofSeconds(maxBackoffSec));

        return this.webClient.get()
            .uri(format(STUDY_ANALYSES_URL_TEMPLATE, repoBaseUrl, studyId, this.indexableStudyStatuses))
            .retrieve()
            .onStatus(HttpStatus.NOT_FOUND::equals,
                clientResponse -> error(notFound(MSG_STUDY_DOES_NOT_EXIST, studyId)))
            .bodyToMono(analysisListType)
            .transform(retryAndTimeout(retryConfig, Duration.ofSeconds(this.studyCallTimeoutSeconds)))
            .doOnSuccess((list) -> log.trace("getStudyAnalyses out, analyses count {} args: {}",
                list.size(), getStudyAnalysesCommand));
    }

    @Override
    @NonNull
    public Flux<Study> getStudies(@NonNull GetAllStudiesCommand getAllStudiesCommand) {
        log.trace("in getStudyAnalyses, args: {} ", getAllStudiesCommand);
        val repoBaseUrl = getAllStudiesCommand.getFilesRepositoryBaseUrl();
        val StringListType = new ParameterizedTypeReference<List<String>>(){};
        val retryConfig = Retry.allBut(NotFoundException.class)
            .retryMax(this.songMaxRetries)
            .doOnRetry(retryCtx -> log.debug("retrying  {}", getAllStudiesCommand))
            .exponentialBackoff(Duration.ofSeconds(minBackoffSec), Duration.ofSeconds(maxBackoffSec));

        return this.webClient.get()
            .uri(format(STUDIES_URL_TEMPLATE, repoBaseUrl))
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            // we need to first parse as mono then stream it as flux because of this :
            // https://github.com/spring-projects/spring-framework/issues/22662
            .bodyToMono(StringListType)
            .transform(retryAndTimeout(retryConfig, Duration.ofSeconds(this.studyCallTimeoutSeconds)))
            .flatMapMany(Flux::fromIterable)
            .map(id -> Study.builder().studyId(id).build());
    }

    @Override
    @NonNull
    public Mono<Analysis> getAnalysis(@NonNull GetAnalysisCommand command) {
        log.trace("in getAnalysis, args: {} ", command);
        val repoBaseUrl = command.getFilesRepositoryBaseUrl();
        val analysisId = command.getAnalysisId();
        val studyId = command.getStudyId();
        val retryConfig = Retry.allBut(NotFoundException.class)
            .retryMax(this.songMaxRetries)
            .doOnRetry(retryCtx -> log.debug("retrying  {}", command))
            .exponentialBackoff(Duration.ofSeconds(minBackoffSec), Duration.ofSeconds(maxBackoffSec));

        return this.webClient.get()
            .uri(format(STUDY_ANALYSIS_URL_TEMPLATE, repoBaseUrl, studyId, analysisId))
            .retrieve()
            .onStatus(HttpStatus.NOT_FOUND::equals,
                clientResponse -> error(notFound(MSG_ANALYSIS_DOES_NOT_EXIST, analysisId, studyId, repoBaseUrl)))
            .bodyToMono(Analysis.class)
            .transform(retryAndTimeout(retryConfig, Duration.ofSeconds(this.analysisCallTimeoutSeconds)))
            .doOnSuccess((analysis) -> log.trace("getAnalysis out, analysis {} args: {}",
                analysis, command))
            .flatMap(analysis -> {
                if (!indexableStudyStatusesList.contains(analysis.getAnalysisState())) {
                    return error(notFound(MSG_ANALYSIS_DOES_NOT_EXIST, analysisId, studyId, repoBaseUrl));
                }
                return Mono.just(analysis);
            });
    }

    @NotNull
    private Mono<Either<IndexerException, List<Analysis>>>
        handleGetAnalysesFailure(GetStudyAnalysesCommand getStudyAnalysesCommand,
                                 @NonNull String studyId, Throwable e) {

        val rootCause = e.getCause() == null ? e : e.getCause();
        val ex = wrapWithIndexerException(rootCause,
            format("failed fetching study analysis, command: {0}, retries exhausted",
                getStudyAnalysesCommand),
            FailureData.builder()
                .failingIds(Map.of(STUDY_ID, Set.of(studyId)))
                .build()
        );
        return Mono.just(Either.left(ex));
    }

    @NotNull
    private Flux<Either<IndexerException, Study>>
        handleGetStudiesFailure(@NonNull GetAllStudiesCommand getAllStudiesCommand, Throwable e) {

        val rootCause = e.getCause() == null ? e : e.getCause();
        val ex = wrapWithIndexerException(rootCause,
        format("failed fetching study analysis, command: {0}, retries exhausted",
            getAllStudiesCommand),
        FailureData.builder()
            .failingIds(Map.of(REPOSITORY,
                Set.of(getAllStudiesCommand.getFilesRepositoryBaseUrl())))
            .build()
        );
        return Flux.just(Either.left(ex));
    }

    private <T> Function<Mono<T>, Mono<T>> retryAndTimeout(Retry<Object> retry, Duration timeout) {
        // order is important here timeouts should be retried.
        return (in) -> in.timeout(timeout).retryWhen(retry);
    }
}
