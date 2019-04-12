package bio.overture.maestro.app.infra.adapter.outbound.notification;

import bio.overture.maestro.domain.api.NotificationChannel;
import bio.overture.maestro.domain.api.NotificationName;
import bio.overture.maestro.domain.port.outbound.notification.IndexerNotification;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.core.annotation.Order;

import java.util.Set;


@Slf4j
@Order(1)
public class LoggingNotificationChannel implements NotificationChannel {

    @Override
    public void send(IndexerNotification notification) {
        val notificationString = notification.toString();
        val notificationStringTruncated = notificationString.substring(0, Math.min(notificationString.length(), 1024));
        switch (notification.getNotificationName().getCategory()) {
            case ERROR:
                log.error("{}", notificationStringTruncated);
                break;
            case WARN:
                log.warn("{}", notificationStringTruncated);
                break;
            default:
                log.info("{}", notificationStringTruncated);
        }
    }

    @Override
    public Set<NotificationName> subscriptions() {
        return Set.of(
            NotificationName.ALL
        );
    }

}
