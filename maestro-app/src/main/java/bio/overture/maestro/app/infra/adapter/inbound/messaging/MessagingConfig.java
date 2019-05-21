package bio.overture.maestro.app.infra.adapter.inbound.messaging;

import bio.overture.maestro.app.infra.adapter.inbound.messaging.song.SongAnalysisStreamListener;
import org.springframework.context.annotation.Import;


@Import({
    IndexingMessagesStreamListener.class,
    SongAnalysisStreamListener.class,
})
public class MessagingConfig { }
