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

package bio.overture.maestro.app.infra.config;

import bio.overture.maestro.app.infra.adapter.inbound.messaging.MessagingConfig;
import bio.overture.maestro.app.infra.adapter.inbound.webapi.GlobalWebExceptionHandler;
import bio.overture.maestro.app.infra.adapter.inbound.webapi.ManagementController;
import bio.overture.maestro.app.infra.adapter.outbound.indexing.elasticsearch.ElasticSearchConfig;
import bio.overture.maestro.app.infra.adapter.outbound.indexing.rules.ExclusionRulesConfig;
import bio.overture.maestro.app.infra.adapter.outbound.metadata.repostiory.RepositoryConfig;
import bio.overture.maestro.app.infra.adapter.outbound.metadata.study.song.SongConfig;
import bio.overture.maestro.app.infra.adapter.outbound.notification.NotificationConfig;
import bio.overture.maestro.app.infra.config.properties.ApplicationProperties;
import bio.overture.maestro.app.infra.config.properties.PropertiesConfig;
import bio.overture.maestro.domain.api.DomainApiConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/** Aggregates all configuration in one place */
@Configuration
@Import({
  DomainApiConfig.class,
  PortsConfig.class,
  InfraConfig.class,
})
public class RootConfiguration {
  public static final String ELASTIC_SEARCH_DOCUMENT_JSON_MAPPER = "documentObjectMapper";
}

/**
 * Configuration about domain related beans (ports implementations), delegated here to keep the
 * domain module agnostic of injection framework .
 */
@Configuration
@Import({
  ElasticSearchConfig.class,
  ExclusionRulesConfig.class,
  MessagingConfig.class,
  WebConfig.class,
  SongConfig.class,
  RepositoryConfig.class,
  NotificationConfig.class,
})
class PortsConfig {}

/**
 * Aggregator for all configurations related to the infrastructure beans (I/O & networking,
 * properties, datasources, etc)
 */
@Configuration
@Import({
  PropertiesConfig.class,
})
class InfraConfig {
  @Bean
  WebClient webClient(ApplicationProperties properties) {
    return WebClient.builder()
        .codecs(c -> c.defaultCodecs().maxInMemorySize(properties.webClientMaxInMemorySize()))
        .build();
  }
}

/** Configuration related to the indexer web api */
@Configuration
@Import({
  GlobalWebExceptionHandler.class,
  ManagementController.class,
})
class WebConfig {
  private static final String DEFAULT_DOCUMENT_JSON_MAPPER = "DEFAULT_DOCUMENT_JSON_MAPPER";

  /**
   * This bean is needed for spring webflux to not use the ELASTIC_SEARCH_DOCUMENT_JSON_MAPPER
   * marked as primary so by default callers who don't specify which bean they need, will get this.
   */
  @Primary
  @Bean(name = DEFAULT_DOCUMENT_JSON_MAPPER)
  public ObjectMapper objectMapper() {
    return new ObjectMapper();
  }

  @Bean
  @ConditionalOnProperty(name = "springdoc.serverOverride.enabled", havingValue = "true")
  public OpenAPI springShopOpenAPI(@Value("${springdoc.serverOverride.value}") String serverOverride) {
    return new OpenAPI()
        .servers(List.of(new Server().url(serverOverride)));
  }

}