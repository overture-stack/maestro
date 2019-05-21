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
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.Set;

/**
 * This channel will store any failure in an append only log file
 * it uses logback loggers to do the write operation instead of manually
 * writing to files.
 *
 * the logs go to separate log file. see logback-spring.xml for the configs.
 */
@Slf4j
@ConditionalOnProperty(value = "maestro.failure-log.enabled", havingValue = "true")
public class FileBasedFailuresLogger implements NotificationChannel {

    @Override
    public void send(IndexerNotification notification) {
        log.error("{}", notification);
    }

    @Override
    public Set<NotificationName> subscriptions() {
        return Set.of(
            NotificationName.ALL
        );
    }
}
