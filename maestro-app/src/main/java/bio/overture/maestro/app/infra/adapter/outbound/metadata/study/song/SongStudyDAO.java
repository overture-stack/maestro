package bio.overture.maestro.app.infra.adapter.outbound.metadata.study.song;

import bio.overture.maestro.app.infra.config.properties.ApplicationProperties;
import bio.overture.maestro.domain.api.exception.FailureData;
import bio.overture.maestro.domain.api.exception.IndexerException;
import bio.overture.maestro.domain.api.exception.NotFoundException;
import bio.overture.maestro.domain.entities.metadata.study.Analysis;
import bio.overture.maestro.domain.entities.metadata.study.Study;
import bio.overture.maestro.domain.port.outbound.metadata.study.GetAllStudiesCommand;
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
import reactor.retry.RetryExhaustedException;

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

    private static final String STUDY_ANALYSES_URL_TEMPLATE = "{0}/studies/{1}/analysis";
    private static final String STUDIES_URL_TEMPLATE = "{0}/studies/all";
    private static final String MSG_STUDY_DOES_NOT_EXIST = "study {0} doesn't exist in the specified repository";
    public static final String STUDY_ID = "studyId";
    public static final String REPOSITORY = "repository";
    private final WebClient webClient;
    private int songMaxRetries;
    private int minBackoffSec = 1;
    private int maxBackoffSec = 5;
    private int songTimeout;

    @Inject
    public SongStudyDAO(WebClient webClient, ApplicationProperties applicationProperties) {
        this.webClient = webClient;
        this.songMaxRetries = applicationProperties.songMaxRetries();
        this.songTimeout = applicationProperties.songTimeoutSeconds();
    }

    @Override
    @NonNull
    public Mono<Either<IndexerException, List<Analysis>>> getStudyAnalyses(GetStudyAnalysesCommand getStudyAnalysesCommand) {
        log.trace("in getStudyAnalyses, args: {} ", getStudyAnalysesCommand);
        val repoBaseUrl = getStudyAnalysesCommand.getFilesRepositoryBaseUrl();
        val studyId = getStudyAnalysesCommand.getStudyId();
        val analysisListType = new ParameterizedTypeReference<List<Analysis>>(){};
        val retryConfig = Retry.allBut(NotFoundException.class)
            .retryMax(this.songMaxRetries)
            .doOnRetry(retryCtx -> log.debug("retrying  {}", getStudyAnalysesCommand))
            .exponentialBackoff(Duration.ofSeconds(minBackoffSec), Duration.ofSeconds(maxBackoffSec));

        return this.webClient.get()
            .uri(format(STUDY_ANALYSES_URL_TEMPLATE, repoBaseUrl, studyId))
            .retrieve()
            .onStatus(HttpStatus.NOT_FOUND::equals,
                clientResponse -> error(notFound(MSG_STUDY_DOES_NOT_EXIST, studyId)))
            .bodyToMono(analysisListType)
            .transform(retryAndTimeout(retryConfig, Duration.ofSeconds(this.songTimeout)))
            .doOnSuccess((list) -> log.trace("getStudyAnalyses out, analyses count {} args: {}",
                list.size(), getStudyAnalysesCommand))
            .map(Either::<IndexerException, List<Analysis>>right)
            .onErrorResume(
                (e) -> e instanceof RetryExhaustedException,
                (e) -> handleGetAnalysesFailure(getStudyAnalysesCommand, studyId, e));
    }

    @Override
    @NonNull
    public Flux<Either<IndexerException, Study>> getStudies(@NonNull GetAllStudiesCommand getAllStudiesCommand) {
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
            .transform(retryAndTimeout(retryConfig, Duration.ofSeconds(this.songTimeout)))
            .flatMapMany(Flux::fromIterable)
            .map(id -> Study.builder().studyId(id).build())
            .map(Either::<IndexerException, Study>right)
            .onErrorResume(
                // wait for retries to finish, if we catch all it wont retry
                (e) -> e instanceof RetryExhaustedException,
                (e) -> handleGetStudiesFailure(getAllStudiesCommand, e)
            );
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
