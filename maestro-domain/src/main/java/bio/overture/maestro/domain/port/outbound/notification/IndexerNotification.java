package bio.overture.maestro.domain.port.outbound.notification;


import bio.overture.maestro.domain.api.NotificationType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class IndexerNotification {
    private NotificationType type;
    private String content;
}
