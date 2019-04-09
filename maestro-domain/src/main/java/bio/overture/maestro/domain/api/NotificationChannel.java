package bio.overture.maestro.domain.api;

import bio.overture.maestro.domain.port.outbound.notification.IndexerNotification;

import java.util.Set;

/**
 * A channel is an abstraction of the technology infrastructure
 * that this notification will be delivered through, can be email, web api call, filesystem or anything.
 */
public interface NotificationChannel {
    void send(IndexerNotification notification);
    Set<NotificationType> subscriptions();
}
