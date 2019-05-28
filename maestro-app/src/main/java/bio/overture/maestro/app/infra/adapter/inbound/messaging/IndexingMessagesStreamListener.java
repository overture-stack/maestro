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

package bio.overture.maestro.app.infra.adapter.inbound.messaging;

import bio.overture.maestro.domain.api.Indexer;
import bio.overture.maestro.domain.api.exception.FailureData;
import bio.overture.maestro.domain.api.message.*;
import io.vavr.Tuple2;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.messaging.handler.annotation.Payload;
import reactor.core.publisher.Mono;

import java.util.function.Function;

import static bio.overture.maestro.app.infra.adapter.inbound.messaging.IndexMessagesHelper.handleIndexResult;

@Slf4j
@EnableBinding(Sink.class)
public class IndexingMessagesStreamListener {

    private final Indexer indexer;

    public IndexingMessagesStreamListener(@NonNull Indexer indexer) {
        this.indexer = indexer;
    }

    @StreamListener(Sink.INPUT)
    public void handleAnalysisMessage(@Payload IndexAnalysisMessage indexAnalysisMessage) {
        handleIndexResult(() -> this.indexOrRemoveAnalysis(indexAnalysisMessage));
    }

    @StreamListener(Sink.INPUT)
    public void handleIndexStudyMessage(@Payload IndexStudyMessage indexStudyMessage) {
        handleIndexResult(() -> this.indexStudy(indexStudyMessage));
    }

    @StreamListener(Sink.INPUT)
    public void handleIndexRepositoryMessage(@Payload IndexRepositoryMessage indexRepositoryMessage) {
        handleIndexResult(() -> this.indexRepository(indexRepositoryMessage));
    }

    private Mono<Tuple2<IndexAnalysisMessage, IndexResult>> indexOrRemoveAnalysis(IndexAnalysisMessage msg) {
        if (msg.getRemove()) {
            return removeAnalysis(msg);
        } else {
            return indexAnalysis(msg);
        }
    }

    private Mono<Tuple2<IndexAnalysisMessage, IndexResult>> removeAnalysis(IndexAnalysisMessage msg) {
        return indexer.removeAnalysis(RemoveAnalysisCommand.builder()
            .analysisIdentifier(AnalysisIdentifier.builder()
                    .studyId(msg.getStudyId())
                    .analysisId(msg.getAnalysisId())
                    .repositoryCode(msg.getRepositoryCode())
                    .build())
                .build())
            .map(out -> new Tuple2<>(msg, out))
            .onErrorResume((e) -> catchUnhandledErrors(msg, e));
    }

    private Mono<Tuple2<IndexAnalysisMessage, IndexResult>> indexAnalysis(IndexAnalysisMessage msg) {
        return indexer.indexAnalysis(IndexAnalysisCommand.builder()
            .analysisIdentifier(AnalysisIdentifier.builder()
                .studyId(msg.getStudyId())
                .analysisId(msg.getAnalysisId())
                .repositoryCode(msg.getRepositoryCode())
                .build()
            ).build())
        .map(out -> new Tuple2<>(msg, out))
        .onErrorResume((e) -> catchUnhandledErrors(msg, e));
    }

    private Mono<Tuple2<IndexStudyMessage, IndexResult>> indexStudy(IndexStudyMessage msg) {
        return indexer.indexStudy(IndexStudyCommand.builder()
                .studyId(msg.getStudyId())
                .repositoryCode(msg.getRepositoryCode())
                .build())
            .map(out -> new Tuple2<>(msg, out));
//            .onErrorResume((e) -> catchUnhandledErrors(msg, e));
    }

    private Mono<Tuple2<IndexRepositoryMessage, IndexResult>> indexRepository(IndexRepositoryMessage msg) {
        return indexer.indexStudyRepository(IndexStudyRepositoryCommand.builder()
                .repositoryCode(msg.getRepositoryCode())
                .build())
            .map(out -> new Tuple2<>(msg, out))
            .onErrorResume((e) -> catchUnhandledErrors(msg, e));
    }

    private <T> Mono<Tuple2<T, IndexResult>> doCheckedCall(T msg, Function<T, Mono<Tuple2<T, IndexResult>>> function) {
        try {
            return function.apply(msg);
        } catch (Exception e) {
            return catchUnhandledErrors(msg, e);
        }
    }

    private <T> Mono<Tuple2<T, IndexResult>> catchUnhandledErrors(T msg, Throwable e) {
        log.error("failed processing message: {} ", msg, e);
        val indexResult = IndexResult.builder()
            .successful(false)
            .failureData(FailureData.builder().build())
            .build();
        return Mono.just(new Tuple2<>(msg, indexResult));
    }

}
