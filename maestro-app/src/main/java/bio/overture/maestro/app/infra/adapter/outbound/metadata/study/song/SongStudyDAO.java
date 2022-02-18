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

package bio.overture.maestro.app.infra.adapter.outbound.metadata.study.song;

import static bio.overture.maestro.domain.utility.Exceptions.notFound;
import static java.text.MessageFormat.format;
import static reactor.core.publisher.Mono.error;

import bio.overture.maestro.app.infra.config.properties.ApplicationProperties;
import bio.overture.maestro.domain.api.exception.NotFoundException;
import bio.overture.maestro.domain.entities.metadata.study.Analysis;
import bio.overture.maestro.domain.entities.metadata.study.GetAnalysisResponse;
import bio.overture.maestro.domain.entities.metadata.study.Study;
import bio.overture.maestro.domain.port.outbound.metadata.study.GetAllStudiesCommand;
import bio.overture.maestro.domain.port.outbound.metadata.study.GetAnalysisCommand;
import bio.overture.maestro.domain.port.outbound.metadata.study.GetStudyAnalysesCommand;
import bio.overture.maestro.domain.port.outbound.metadata.study.StudyDAO;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import javax.inject.Inject;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.retry.Retry;

@Slf4j
class SongStudyDAO implements StudyDAO {

  private static final String STUDY_ANALYSES_URL_TEMPLATE =
      "{0}/studies/{1}/analysis/paginated?analysisStates={2}&limit={3}&offset={4}";
  private static final String STUDY_ANALYSIS_URL_TEMPLATE = "{0}/studies/{1}/analysis/{2}";
  private static final String STUDIES_URL_TEMPLATE = "{0}/studies/all";
  private static final String MSG_STUDY_DOES_NOT_EXIST =
      "studyId {0} doesn't exist  in the specified repository";
  private static final String MSG_ANALYSIS_DOES_NOT_EXIST =
      "analysis {0} doesn't exist for studyId {1}, repository {2} (or not in a matching state)";
  private static final int FALLBACK_SONG_TIMEOUT = 60;
  private static final int FALLBACK_SONG_ANALYSIS_TIMEOUT = 5;
  private static final int FALLBACK_SONG_MAX_RETRY = 0;
  private static final int DEFAULT_SONG_PAGE_LIMIT = 25;
  private final WebClient webClient;
  private final int songMaxRetries;
  private final int minBackoffSec = 1;
  private final int maxBackoffSec = 5;
  private final String indexableStudyStatuses;
  private final List<String> indexableStudyStatusesList;
  /** must be bigger than 0 or all calls will fail */
  private final int studyCallTimeoutSeconds;

  private final int analysisCallTimeoutSeconds;
  private final int pageLimit;

  @Inject
  public SongStudyDAO(
      @NonNull WebClient webClient, @NonNull ApplicationProperties applicationProperties) {
    this.webClient = webClient;
    this.pageLimit =
        applicationProperties.pageLimit() > 0
            ? applicationProperties.pageLimit()
            : DEFAULT_SONG_PAGE_LIMIT;
    this.indexableStudyStatuses = applicationProperties.indexableStudyStatuses();
    this.indexableStudyStatusesList = List.of(indexableStudyStatuses.split(","));
    this.songMaxRetries =
        applicationProperties.songMaxRetries() >= 0
            ? applicationProperties.songMaxRetries()
            : FALLBACK_SONG_MAX_RETRY;
    this.studyCallTimeoutSeconds =
        applicationProperties.songStudyCallTimeoutSeconds() > 0
            ? applicationProperties.songStudyCallTimeoutSeconds()
            : FALLBACK_SONG_TIMEOUT;
    this.analysisCallTimeoutSeconds =
        applicationProperties.songAnalysisCallTimeoutSeconds() > 0
            ? applicationProperties.songAnalysisCallTimeoutSeconds()
            : FALLBACK_SONG_ANALYSIS_TIMEOUT;
  }

  @Override
  public Mono<List<Analysis>> getStudyAnalyses(GetStudyAnalysesCommand getStudyAnalysesCommand) {
    log.trace("in getStudyAnalyses, args: {} ", getStudyAnalysesCommand);
    val repoBaseUrl = getStudyAnalysesCommand.getFilesRepositoryBaseUrl();
    val studyId = getStudyAnalysesCommand.getStudyId();

    var initialOffset = 0;
    val url =
        format(
            STUDY_ANALYSES_URL_TEMPLATE,
            repoBaseUrl,
            studyId,
            this.indexableStudyStatuses,
            this.pageLimit,
            initialOffset);
    val threadSafeOffset = new AtomicInteger(0);

    return fetchItems(url, studyId)
        // The expand method recursively calls fetchItems() and emits response of first page to the
        // last.
        // the first request being made is offset = 0, and the second request is offset = 25,
        // and all the way to the last page.
        .expand(
            rep -> {
              if (rep.getAnalyses().size() == 0) {
                return Mono.empty();
              }
              threadSafeOffset.addAndGet(this.pageLimit);
              val currentUrl =
                  format(
                      STUDY_ANALYSES_URL_TEMPLATE,
                      repoBaseUrl,
                      studyId,
                      this.indexableStudyStatuses,
                      this.pageLimit,
                      threadSafeOffset.get());
              return fetchItems(currentUrl, studyId);
            })
        .flatMap(rep -> Flux.fromIterable(rep.getAnalyses()))
        .collectList()
        .doOnSuccess(
            (list) ->
                log.trace(
                    "getStudyAnalyses out, analyses count {} args: {}",
                    list.size(),
                    getStudyAnalysesCommand));
  }

  private Mono<GetAnalysisResponse> fetchItems(@NonNull String url, @NonNull String studyId) {
    log.trace("get paged analyses, url = {}", url);
    val retryConfig =
        Retry.allBut(NotFoundException.class)
            .retryMax(this.songMaxRetries)
            .doOnRetry(
                retryCtx ->
                    log.error("exception happened, retrying  {}", url, retryCtx.exception()))
            .exponentialBackoff(
                Duration.ofSeconds(minBackoffSec), Duration.ofSeconds(maxBackoffSec));
    return this.webClient
        .get()
        .uri(url)
        .retrieve()
        .onStatus(
            HttpStatus.NOT_FOUND::equals,
            clientResponse -> error(notFound(MSG_STUDY_DOES_NOT_EXIST, studyId)))
        .bodyToMono(GetAnalysisResponse.class)
        .transform(retryAndTimeout(retryConfig, Duration.ofSeconds(this.studyCallTimeoutSeconds)));
  }

  @Override
  public Flux<Study> getStudies(@NonNull GetAllStudiesCommand getAllStudiesCommand) {
    log.trace("in getStudyAnalyses, args: {} ", getAllStudiesCommand);
    val repoBaseUrl = getAllStudiesCommand.getFilesRepositoryBaseUrl();
    val StringListType = new ParameterizedTypeReference<List<String>>() {};
    val retryConfig =
        Retry.allBut(NotFoundException.class)
            .retryMax(this.songMaxRetries)
            .doOnRetry(
                retryCtx -> log.error("retrying  {}", getAllStudiesCommand, retryCtx.exception()))
            .exponentialBackoff(
                Duration.ofSeconds(minBackoffSec), Duration.ofSeconds(maxBackoffSec));

    return this.webClient
        .get()
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
  public Mono<Analysis> getAnalysis(@NonNull GetAnalysisCommand command) {
    log.trace("in getAnalysis, args: {} ", command);
    val repoBaseUrl = command.getFilesRepositoryBaseUrl();
    val analysisId = command.getAnalysisId();
    val studyId = command.getStudyId();
    val retryConfig =
        Retry.allBut(NotFoundException.class)
            .retryMax(this.songMaxRetries)
            .doOnRetry(
                retryCtx ->
                    log.error("exception happened, retrying  {}", command, retryCtx.exception()))
            .exponentialBackoff(
                Duration.ofSeconds(minBackoffSec), Duration.ofSeconds(maxBackoffSec));

    return this.webClient
        .get()
        .uri(format(STUDY_ANALYSIS_URL_TEMPLATE, repoBaseUrl, studyId, analysisId))
        .retrieve()
        .onStatus(
            HttpStatus.NOT_FOUND::equals,
            clientResponse ->
                error(notFound(MSG_ANALYSIS_DOES_NOT_EXIST, analysisId, studyId, repoBaseUrl)))
        .bodyToMono(Analysis.class)
        .transform(
            retryAndTimeout(retryConfig, Duration.ofSeconds(this.analysisCallTimeoutSeconds)))
        .doOnSuccess(
            (analysis) -> log.trace("getAnalysis out, analysis {} args: {}", analysis, command))
        .flatMap(
            analysis -> {
              if (!indexableStudyStatusesList.contains(analysis.getAnalysisState())) {
                return error(
                    notFound(MSG_ANALYSIS_DOES_NOT_EXIST, analysisId, studyId, repoBaseUrl));
              }
              return Mono.just(analysis);
            });
  }

  private <T> Function<Mono<T>, Mono<T>> retryAndTimeout(Retry<Object> retry, Duration timeout) {
    // order is important here timeouts should be retried.
    return (in) -> in.timeout(timeout).retryWhen(retry);
  }
}
