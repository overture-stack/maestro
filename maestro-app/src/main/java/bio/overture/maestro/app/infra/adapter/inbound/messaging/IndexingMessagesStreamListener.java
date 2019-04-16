package bio.overture.maestro.app.infra.adapter.inbound.messaging;

import bio.overture.maestro.domain.api.Indexer;
import bio.overture.maestro.domain.api.exception.IndexerException;
import bio.overture.maestro.domain.api.message.IndexStudyCommand;
import bio.overture.maestro.domain.api.message.IndexStudyRepositoryCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@EnableBinding(Sink.class)
public class IndexingMessagesStreamListener {

    private Indexer indexer;

    public IndexingMessagesStreamListener(Indexer indexer) {
        this.indexer = indexer;
    }


    public void handleIndexAnalysisMessage(@Input(Sink.INPUT) Flux<IndexAnalysisMessage> indexAnalysisMessage) {
        throw new IndexerException("not implemented yet");
    }

    @StreamListener
    public void handleIndexStudyMessage(@Input(Sink.INPUT) Flux<IndexStudyMessage> indexStudyMessageFlux) {
        indexStudyMessageFlux.subscribe( msg -> {
            try {
                indexer.indexStudy(IndexStudyCommand.builder()
                    .studyId(msg.getStudyId())
                    .repositoryCode(msg.getRepositoryCode())
                    .build()
                ).onErrorResume((e) -> {
                    log.error("failed reading message: {} ", msg, e);
                    return Mono.empty();
                }).subscribe(indexResult ->
                    log.info(" processed message : {} success : {}", msg, indexResult.isSuccessful())
                );
            } catch (Exception e) {
                log.error("failed reading message: {} ", msg, e);
            }
        });
    }

    @StreamListener
    public void handleIndexRepositoryMessage(@Input(Sink.INPUT) Flux<IndexRepositoryMessage> indexRepoMessageFlux) {
        indexRepoMessageFlux.subscribe( msg -> {
            try {
                indexer.indexStudyRepository(IndexStudyRepositoryCommand.builder()
                    .repositoryCode(msg.getRepositoryCode())
                    .build()
                ).onErrorResume((e) -> {
                    log.error("failed reading message: {} ", msg, e);
                    return Mono.empty();
                }).subscribe(indexResult ->
                    log.info(" processed message : {} success : {}", msg, indexResult.isSuccessful())
                );
            } catch (Exception e) {
                log.error("failed reading message: {} ", msg, e);
            }
        });
    }
}
