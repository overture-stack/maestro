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

import static bio.overture.maestro.app.infra.config.properties.ApplicationProperties.FAILURE_LOG_PROP_KEY;

import bio.overture.maestro.domain.api.NotificationChannel;
import bio.overture.maestro.domain.api.NotificationName;
import bio.overture.maestro.domain.port.outbound.notification.IndexerNotification;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import reactor.core.publisher.Mono;

/**
 * This channel will store any failure in an append only log files it uses logback loggers to do the
 * write operation instead of manually writing to files.
 *
 * <p>the logs go to separate log files. see logback-spring.xml for the configs.
 */
@Slf4j
@ConditionalOnProperty(value = FAILURE_LOG_PROP_KEY, havingValue = "true")
public class FileBasedFailuresLogger implements NotificationChannel {

  @Override
  public Mono<Boolean> send(IndexerNotification notification) {
    log.error("{}", notification);
    return Mono.just(true);
  }

  @Override
  public Set<NotificationName> subscriptions() {
    return Set.of(NotificationName.ALL);
  }
}
