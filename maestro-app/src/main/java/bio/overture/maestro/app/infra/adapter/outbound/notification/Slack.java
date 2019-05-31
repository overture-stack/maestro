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
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.inject.Inject;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static bio.overture.maestro.app.infra.config.properties.ApplicationProperties.MAESTRO_NOTIFICATIONS_SLACK_ENABLED_PROP_KEY;

@Slf4j
@ConditionalOnProperty(value = MAESTRO_NOTIFICATIONS_SLACK_ENABLED_PROP_KEY, havingValue = "true")
public class Slack implements NotificationChannel {

    private static final String TYPE = "##TYPE##";
    private static final String DATA = "##DATA##";
    private final WebClient webClient;
    private final SlackChannelInfo slackChannelInfo;

    @Inject
    public Slack(ApplicationProperties properties, WebClient webClient) {
        this.webClient = webClient;
        this.slackChannelInfo = properties.getSlackChannelInfo();
    }

    @Override
    public Mono<Boolean> send(@NonNull IndexerNotification notification) {
        var template = this.slackChannelInfo.infoTemplate();
        switch (notification.getNotificationName().getCategory()) {
            case ERROR:
                template = this.slackChannelInfo.errorTemplate(); break;
            case WARN:
                template = this.slackChannelInfo.warningTemplate(); break;
        }

        val dataString = notification.getAttributes().toString();
        val dataStringTruncated = dataString.substring(0,
            Math.min(dataString.length(), this.slackChannelInfo.maxDataLength()));

        val text = template.
            replace(TYPE, notification.getNotificationName().name())
            .replace(DATA, dataStringTruncated);

        val payload = Map.of(
            "text", text,
            "channel", this.slackChannelInfo.channel(),
            "username", this.slackChannelInfo.username()
        );

        return this.webClient.post()
            .uri(this.slackChannelInfo.url())
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromObject(payload))
            .retrieve()
            .bodyToMono(String.class)
            .timeout(Duration.ofSeconds(3L))
            .map((ignored) -> true)
            .onErrorResume(e -> {
                log.error("failed to send message to slack", e);
                return Mono.just(false);
            });
    }

    @Override
    public Set<NotificationName> subscriptions() {
        return Set.copyOf(slackChannelInfo.subscriptions());
    }

    public interface SlackChannelInfo {
        String url();
        String channel();
        String username();
        String errorTemplate();
        String warningTemplate();
        String infoTemplate();
        int maxDataLength();
        Set<NotificationName> subscriptions();
    }
}
