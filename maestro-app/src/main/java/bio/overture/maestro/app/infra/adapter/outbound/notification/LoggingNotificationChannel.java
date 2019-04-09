package bio.overture.maestro.app.infra.adapter.outbound.notification;

import bio.overture.maestro.domain.api.NotificationChannel;
import bio.overture.maestro.domain.api.NotificationType;
import bio.overture.maestro.domain.port.outbound.notification.IndexerNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;

import java.util.Set;


@Slf4j
@Order(1)
public class LoggingNotificationChannel implements NotificationChannel {

    @Override
    public void send(IndexerNotification notification) {
        log.info("received this notification: {}", notification);
    }

    @Override
    public Set<NotificationType> subscriptions() {
        return Set.of(NotificationType.ALL);
    }

}
