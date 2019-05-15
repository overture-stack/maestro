package bio.overture.maestro.app.infra.adapter.inbound.messaging;

import bio.overture.maestro.domain.api.Indexer;
import bio.overture.maestro.domain.api.exception.FailureData;
import bio.overture.maestro.domain.api.message.*;
import io.vavr.Tuple2;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

@Slf4j
@EnableBinding(Sink.class)
public class IndexingMessagesStreamListener {

    private final Indexer indexer;

    public IndexingMessagesStreamListener(Indexer indexer) {
        this.indexer = indexer;
    }

    @StreamListener
    public void handleRemoveAnalysisMessage(@Input(Sink.INPUT) Flux<IndexAnalysisMessage> indexAnalysisMessageFlux) {
        indexAnalysisMessageFlux.flatMap(msg-> this.doCheckedCall(msg, this::indexOrRemoveAnalysis))
            .subscribe(tuple -> log.info(" processed message : {} success : {}", tuple._1(), tuple._2().isSuccessful()));
    }

    @StreamListener
    public void handleIndexStudyMessage(@Input(Sink.INPUT) Flux<IndexStudyMessage> indexStudyMessageFlux) {
        indexStudyMessageFlux.flatMap(msg-> this.doCheckedCall(msg, this::indexStudy))
            .subscribe(tuple -> log.info(" processed message : {} success : {}", tuple._1(), tuple._2().isSuccessful()));
    }

    @StreamListener
    public void handleIndexRepositoryMessage(@Input(Sink.INPUT) Flux<IndexRepositoryMessage> indexRepoMessageFlux) {
        indexRepoMessageFlux.flatMap(msg-> this.doCheckedCall(msg, this::indexRepository))
            .subscribe(tuple -> log.info(" processed message : {} success : {}", tuple._1(), tuple._2().isSuccessful()));
    }

    @NotNull
    private Mono<Tuple2<IndexAnalysisMessage, IndexResult>> indexOrRemoveAnalysis(IndexAnalysisMessage msg) {
        if (msg.getRemove()) {
            return removeAnalysis(msg);
        } else {
            return indexAnalysis(msg);
        }
    }

    @NotNull
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

    @NotNull
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

    @NotNull
    private Mono<Tuple2<IndexStudyMessage, IndexResult>> indexStudy(IndexStudyMessage msg) {
        return indexer.indexStudy(IndexStudyCommand.builder()
                .studyId(msg.getStudyId())
                .repositoryCode(msg.getRepositoryCode())
                .build())
            .map(out -> new Tuple2<>(msg, out))
            .onErrorResume((e) -> catchUnhandledErrors(msg, e));
    }

    @NotNull
    private Mono<Tuple2<IndexRepositoryMessage, IndexResult>> indexRepository(IndexRepositoryMessage msg) {
        return indexer.indexStudyRepository(IndexStudyRepositoryCommand.builder()
                .repositoryCode(msg.getRepositoryCode())
                .build())
            .map(out -> new Tuple2<>(msg, out))
            .onErrorResume((e) -> catchUnhandledErrors(msg, e));
    }

    @NotNull
    private <T> Mono<Tuple2<T, IndexResult>> catchUnhandledErrors(T msg, Throwable e) {
        log.error("failed processing message: {} ", msg, e);
        val indexResult = IndexResult.builder()
            .successful(false)
            .failureData(FailureData.builder().build())
            .build();
        return Mono.just(new Tuple2<>(msg, indexResult));
    }

    private <T> Mono<Tuple2<T, IndexResult>>
        doCheckedCall(T msg, Function<T, Mono<Tuple2<T, IndexResult>>> function) {
        try {
            return function.apply(msg);
        } catch (Exception e) {
            return catchUnhandledErrors(msg, e);
        }
    }

}
