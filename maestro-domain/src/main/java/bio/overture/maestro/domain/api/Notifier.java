package bio.overture.maestro.domain.api;

import bio.overture.maestro.domain.port.outbound.notification.IndexerNotification;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.Set;

@Slf4j
class Notifier {

    private final Set<NotificationChannel> notificationChannels;

    @Inject
    public Notifier(Set<NotificationChannel> notificationChannels) {
        this.notificationChannels = notificationChannels;
    }

    void notify(IndexerNotification notification) {
        notificationChannels.stream()
            .filter(notificationChannel ->
                notificationChannel.subscriptions().contains(NotificationName.ALL)
                    || notificationChannel.subscriptions().contains(notification.getNotificationName()))
            .forEach(notificationChannel -> {
                try {
                    notificationChannel.send(notification);
                } catch (Exception e) {
                    log.error("failed to deliver notification {} to channel {}", notification, notificationChannel, e);
                }
            });
    }
}
