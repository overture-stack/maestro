package bio.overture.maestro.domain.api;

import bio.overture.maestro.domain.port.outbound.notification.IndexerNotification;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.Set;

@Slf4j
class Notifier {

    private Set<NotificationChannel> notificationChannels;

    @Inject
    public Notifier(Set<NotificationChannel> notificationChannels) {
        this.notificationChannels = notificationChannels;
    }

    void notify(IndexerNotification notification) {
        notificationChannels.stream()
            .filter(notificationChannel ->
                notificationChannel.subscriptions().contains(NotificationType.ALL)
                    || notificationChannel.subscriptions().contains(notification.getType()))
            .forEach(notificationChannel -> {
                try {
                    notificationChannel.send(notification);
                } catch (Exception e) {
                    log.error("failed to deliver notification {} to channel {}", notification, notificationChannel, e);
                }
            });
    }
}
