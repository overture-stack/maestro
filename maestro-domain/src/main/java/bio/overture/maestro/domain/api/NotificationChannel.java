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
import lombok.NonNull;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * A channel is an abstraction of the technology infrastructure
 * that this notification will be delivered through, can be email, web api call, filesystem or anything.
 */
public interface NotificationChannel {
    Mono<Boolean> send(@NonNull IndexerNotification notification);
    Set<NotificationName> subscriptions();
}