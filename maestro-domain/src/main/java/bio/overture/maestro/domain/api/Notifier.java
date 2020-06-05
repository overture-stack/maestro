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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
class Notifier {

  private final Set<NotificationChannel> notificationChannels;

  @Inject
  public Notifier(Set<NotificationChannel> notificationChannels) {
    this.notificationChannels = notificationChannels;
  }

  /**
   * Asynchronously calls the eligible notification channels.
   *
   * @param notification the notification to send
   */
  public void notify(IndexerNotification notification) {
    // some of the channels may need async I/O execution (slack) but
    // the caller (indexer) doesn't need to worry about what happens here so no need to return the
    // flux and
    // we subscribe here.
    Flux.fromIterable(getEligibleChannels(notification))
        .flatMap(
            notificationChannel ->
                sendNotificationThroughChannel(notification, notificationChannel))
        .subscribe();
  }

  private Mono<Boolean> sendNotificationThroughChannel(
      IndexerNotification notification, NotificationChannel notificationChannel) {
    return notificationChannel
        .send(notification)
        .onErrorResume(
            e -> {
              log.error(
                  "failed to deliver notification {} to channel {}",
                  notification,
                  notificationChannel,
                  e);
              return Mono.just(false);
            });
  }

  private List<NotificationChannel> getEligibleChannels(IndexerNotification notification) {
    return notificationChannels.stream()
        .filter(notificationChannel -> shouldReceiveNotification(notification, notificationChannel))
        .collect(Collectors.toUnmodifiableList());
  }

  private boolean shouldReceiveNotification(
      IndexerNotification notification, NotificationChannel notificationChannel) {
    return notificationChannel.subscriptions().contains(NotificationName.ALL)
        || notificationChannel.subscriptions().contains(notification.getNotificationName());
  }
}
