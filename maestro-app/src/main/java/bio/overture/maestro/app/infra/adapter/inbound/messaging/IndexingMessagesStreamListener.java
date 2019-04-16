package bio.overture.maestro.app.infra.adapter.inbound.messaging;

import bio.overture.maestro.domain.api.Indexer;
import bio.overture.maestro.domain.api.message.IndexStudyCommand;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.messaging.handler.annotation.Payload;
import reactor.core.publisher.Flux;

public class IndexingMessagesStreamListener {

    private Indexer indexer;

    public IndexingMessagesStreamListener(Indexer indexer) {
        this.indexer = indexer;
    }
    public void handleIndexAnalysisMessage(@Payload IndexAnalysisMessage indexAnalysisMessage) {

    }

    @StreamListener
    public void handleIndexStudyMessage(@Input(Sink.INPUT) Flux<IndexStudyMessage> indexStudyMessageFlux) {
        indexStudyMessageFlux.doOnNext(
            msg -> indexer.indexStudy(IndexStudyCommand.builder()
                .studyId(msg.getStudyId())
                .repositoryCode(msg.getRepositoryCode())
                .build()
            ));
    }

    public void handleIndexRepositoryMessage(@Input(Sink.INPUT) Flux<IndexRepositoryMessage> indexRepoMessageFlux) {
        indexRepoMessageFlux.doOnNext(
            msg -> indexer.indexStudy(IndexStudyCommand.builder()
                .repositoryCode(msg.getRepositoryCode())
                .build()
            ));
    }
}
