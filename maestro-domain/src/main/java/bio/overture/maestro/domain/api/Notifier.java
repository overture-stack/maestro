/*
 *  Copyright (c) 2019. Ontario Institute for Cancer Research
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as
 *   published by the Free Software Foundation, either version 3 of the
 *   License, or (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
