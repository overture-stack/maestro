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

package bio.overture.maestro.app.infra.adapter.outbound.notification;

import bio.overture.maestro.domain.api.NotificationChannel;
import bio.overture.maestro.domain.api.NotificationName;
import bio.overture.maestro.domain.port.outbound.notification.IndexerNotification;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.core.annotation.Order;
import reactor.core.publisher.Mono;

/**
 * This channel is to log the failures to the standard logger (console). it's different than the
 * FileBasedFailuresLogger in the fact that this is not persistent or for failures review.
 */
@Slf4j
@Order(1)
public class LoggingNotificationChannel implements NotificationChannel {

  private static final int MAX_NOTIFICATION_STRING_LENGTH = 1024;

  @Override
  public Mono<Boolean> send(IndexerNotification notification) {
    val notificationString = notification.toString();
    val notificationStringTruncated =
        notificationString.substring(
            0, Math.min(notificationString.length(), MAX_NOTIFICATION_STRING_LENGTH));

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

    return Mono.just(true);
  }

  @Override
  public Set<NotificationName> subscriptions() {
    return Set.of(NotificationName.ALL);
  }
}
