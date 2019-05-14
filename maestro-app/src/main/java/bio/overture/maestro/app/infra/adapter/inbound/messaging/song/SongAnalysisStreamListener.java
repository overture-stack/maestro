package bio.overture.maestro.app.infra.adapter.inbound.messaging.song;

import bio.overture.maestro.domain.api.Indexer;
import bio.overture.maestro.domain.api.message.AnalysisIdentifier;
import bio.overture.maestro.domain.api.message.IndexAnalysisCommand;
import bio.overture.maestro.domain.api.message.IndexResult;
import bio.overture.maestro.domain.api.message.RemoveAnalysisCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@EnableBinding(SongAnalysisSink.class)
public class SongAnalysisStreamListener {

    private Indexer indexer;

    public SongAnalysisStreamListener(Indexer indexer) {
        this.indexer = indexer;
    }

    @StreamListener
    public void handleMessage(@Input(SongAnalysisSink.NAME) Flux<AnalysisMessage> analysisMessageFlux) {
        analysisMessageFlux.subscribe(this::doHandle);
    }

    private void doHandle(AnalysisMessage msg) {
        Mono<IndexResult> resultMono;
        try {
            if (msg.getState().equals("PUBLISHED")) {
                resultMono = indexer.indexAnalysis(IndexAnalysisCommand.builder()
                    .analysisIdentifier(AnalysisIdentifier.builder()
                        .repositoryCode(msg.getSongServerId())
                        .studyId(msg.getStudyId())
                        .analysisId(msg.getAnalysisId())
                        .build())
                    .build());
            } else {
                resultMono = indexer.removeAnalysis(RemoveAnalysisCommand.builder()
                    .analysisIdentifier(AnalysisIdentifier.builder()
                        .analysisId(msg.getAnalysisId())
                        .studyId(msg.getStudyId())
                        .repositoryCode(msg.getSongServerId())
                        .build()
                    ).build());
            }
            resultMono.onErrorResume((e) -> {
                log.error("failed reading message: {} ", msg, e);
                return Mono.empty();
            }).subscribe(indexResult -> log.info(" processed message : {} success : {}",
                msg, indexResult.isSuccessful()));
        } catch (Exception e) {
            log.error("failed reading message: {} ", msg, e);
        }
    }
}
