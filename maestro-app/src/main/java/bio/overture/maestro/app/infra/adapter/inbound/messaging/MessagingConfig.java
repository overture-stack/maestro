package bio.overture.maestro.app.infra.adapter.inbound.messaging;

import org.springframework.context.annotation.Import;


@Import({
    IndexingMessagesStreamListener.class,
})
public class MessagingConfig {}
