package bio.overture.maestro.app.infra.adapter.inbound.messaging.song;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.messaging.SubscribableChannel;

public interface SongAnalysisSink {
    /**
     * Input channel name.
     */
    String NAME = "song-input";

    /**
     * @return input channel.
     */
    @Input(SongAnalysisSink.NAME)
    SubscribableChannel songInput();

}
