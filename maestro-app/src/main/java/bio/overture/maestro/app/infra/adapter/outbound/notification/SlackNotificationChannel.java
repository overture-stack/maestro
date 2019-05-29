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

import bio.overture.maestro.app.infra.config.properties.ApplicationProperties;
import bio.overture.maestro.domain.api.NotificationChannel;
import bio.overture.maestro.domain.api.NotificationName;
import bio.overture.maestro.domain.port.outbound.notification.IndexerNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import javax.inject.Inject;
import java.util.Set;

import static bio.overture.maestro.app.infra.config.properties.ApplicationProperties.MAESTRO_NOTIFICATIONS_SLACK_ENABLED;

@Slf4j
@ConditionalOnProperty(value = MAESTRO_NOTIFICATIONS_SLACK_ENABLED, havingValue = "true")
public class SlackNotificationChannel implements NotificationChannel {

    @Inject
    public SlackNotificationChannel(ApplicationProperties properties) {

    }

    @Override
    public void send(IndexerNotification notification) {

    }

    @Override
    public Set<NotificationName> subscriptions() {
        return Set.of(NotificationName.ALL);
    }
}
