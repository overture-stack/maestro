package bio.overture.maestro.app.infra.adapter.inbound.messaging;

import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.context.annotation.Import;

@EnableBinding(Sink.class)
@Import({
    IndexingMessagesStreamListener.class,
})
public class MessagingConfig {}
