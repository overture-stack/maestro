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

package bio.overture.maestro.app.infra.config.properties;

import static bio.overture.maestro.app.infra.config.properties.DefaultApplicationProperties.MAESTRO_PREFIX;

import bio.overture.maestro.app.infra.adapter.outbound.notification.Slack;
import bio.overture.maestro.domain.api.NotificationName;
import bio.overture.maestro.domain.entities.indexing.StorageType;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * This abstracts the application from the underlying property source and allows for easier
 * testability by mocking/proxying to this class if needed.
 */
@Component
@Setter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@ConfigurationProperties(prefix = MAESTRO_PREFIX, ignoreInvalidFields = true)
final class DefaultApplicationProperties implements ApplicationProperties {

  static final String MAESTRO_PREFIX = "maestro";

  private WebClient webClient = new WebClient();

  @Override
  public int webClientMaxInMemorySize() {
    return this.webClient.maxInMemorySize;
  }

  @Override
  public List<String> elasticSearchClusterNodes() {
    return List.copyOf(this.elasticsearch.getClusterNodes());
  }

  @Override
  public String fileCentricAlias() {
    return this.elasticsearch.getIndexes().getFileCentric().getAlias();
  }

  @Override
  public String fileCentricIndexName() {
    return this.elasticsearch.getIndexes().getFileCentric().getName();
  }

  @Override
  public boolean isFileCentricIndexEnabled() {
    return this.elasticsearch.getIndexes().getFileCentric().isEnabled();
  }

  @Override
  public String analysisCentricAlias() {
    return this.elasticsearch.getIndexes().getAnalysisCentric().getAlias();
  }

  @Override
  public String analysisCentricIndexName() {
    return this.elasticsearch.getIndexes().getAnalysisCentric().getName();
  }

  @Override
  public boolean isAnalysisCentricIndexEnabled() {
    return this.elasticsearch.getIndexes().getAnalysisCentric().isEnabled();
  }

  @Override
  public int maxDocsPerBulkRequest() {
    return this.elasticsearch.getClient().getDocsPerBulkReqMax();
  }

  @Override
  public int elasticSearchClientConnectionTimeoutMillis() {
    return this.elasticsearch.getClient().getConnectionTimeout();
  }

  @Override
  public int elasticSearchClientSocketTimeoutMillis() {
    return this.elasticsearch.getClient().getSocketTimeout();
  }

  @Override
  public boolean elasticSearchTlsTrustSelfSigned() {
    return this.elasticsearch.getClient().isTrustSelfSignedCert();
  }

  @Override
  public boolean elasticSearchBasicAuthEnabled() {
    return this.elasticsearch.getClient().getBasicAuth().isEnabled();
  }

  @Override
  public String elasticSearchAuthUser() {
    return this.elasticsearch.getClient().getBasicAuth().getUser();
  }

  @Override
  public String elasticSearchAuthPassword() {
    return this.elasticsearch.getClient().getBasicAuth().getPassword();
  }

  @Override
  public String elasticSearchPathPrefix() {
    return this.elasticsearch.getPathPrefix();
  }

  @Override
  public long elasticSearchRetryWaitDurationMillis() {
    return this.elasticsearch.getClient().getRetry().getWaitDurationMillis();
  }

  @Override
  public int elasticSearchRetryMaxAttempts() {
    return this.elasticsearch.getClient().getRetry().getMaxAttempts();
  }

  @Override
  public List<PropertiesFileRepository> repositories() {
    return List.copyOf(this.repositories);
  }

  @Override
  public Resource fileCentricIndex() {
    return fileCentricIndex;
  }

  @Override
  public Resource analysisCentricIndex() {
    return analysisCentricIndex;
  }

  @Override
  public Map<String, List<String>> idExclusionRules() {
    return Map.copyOf(this.exclusionRules.getById());
  }

  @Override
  public int songMaxRetries() {
    return this.song.getMaxRetries();
  }

  @Override
  public int songStudyCallTimeoutSeconds() {
    return this.song.getTimeoutSec().getStudy();
  }

  @Override
  public String indexableStudyStatuses() {
    return this.song.getIndexableStudyStatesCsv();
  }

  @Override
  public int pageLimit() {
    return this.song.getPageLimit();
  }

  @Override
  public int songAnalysisCallTimeoutSeconds() {
    return this.song.getTimeoutSec().getAnalysis();
  }

  @Override
  public Slack.SlackChannelInfo getSlackChannelInfo() {
    return new Slack.SlackChannelInfo() {
      @Override
      public String url() {
        return notifications.getSlack().getUrl();
      }

      @Override
      public String channel() {
        return notifications.getSlack().getChannel();
      }

      @Override
      public String username() {
        return notifications.getSlack().getUsername();
      }

      @Override
      public String errorTemplate() {
        return notifications.getSlack().getTemplates().getError();
      }

      @Override
      public String warningTemplate() {
        return notifications.getSlack().getTemplates().getWarning();
      }

      @Override
      public String infoTemplate() {
        return notifications.getSlack().getTemplates().getInfo();
      }

      @Override
      public int maxDataLength() {
        return notifications.getSlack().getMaxDataLength();
      }

      @Override
      public Set<NotificationName> subscriptions() {
        return notifications.getSlack().getNotifiedOn().stream()
            .map(NotificationName::valueOf)
            .collect(Collectors.toUnmodifiableSet());
      }
    };
  }

  @Override
  public boolean disableIndexing() {
    return this.disableIndexing;
  }

  @Value("classpath:file_centric.json")
  private Resource fileCentricIndex;

  @Value("classpath:analysis_centric.json")
  private Resource analysisCentricIndex;

  private Song song = new Song();
  private Elasticsearch elasticsearch = new Elasticsearch();
  private List<DefaultPropertiesFileRepository> repositories;
  private ExclusionRules exclusionRules = new ExclusionRules();
  private Notifications notifications = new Notifications();
  private boolean disableIndexing = false;

  @Data
  @ToString
  @EqualsAndHashCode
  private static class DefaultPropertiesFileRepository implements PropertiesFileRepository {
    private String name;
    private String code;
    private String url;
    private String dataPath = "/oicr.icgc/data";
    private String metadataPath = "/oicr.icgc.meta/metadata";
    private String organization;
    private String country;
    private StorageType storageType = StorageType.S3;
  }

  @Data
  @ToString
  @EqualsAndHashCode
  private static class WebClient {
    private int maxInMemorySize = -1;
  }

  @Data
  @ToString
  @EqualsAndHashCode
  private static class Song {
    private SongTimeouts timeoutSec = new SongTimeouts();
    private int maxRetries = 3;
    // FIXME: This configuration is called three different things in this codebase
    private String indexableStudyStatesCsv = "PUBLISHED";
    private int pageLimit = 25;
  }

  @Data
  @ToString
  @EqualsAndHashCode
  private static class SongTimeouts {
    private int study = 20;
    private int analysis = 5;
  }

  @Data
  @ToString
  @EqualsAndHashCode
  private static class ExclusionRules {
    Map<String, List<String>> byId = Map.of();
  }

  @Data
  @ToString
  @EqualsAndHashCode
  private static class Elasticsearch {
    private List<String> clusterNodes = List.of("localhost:9200");
    private Indexes indexes = new Indexes();
    private String pathPrefix = "";
    private ElasticsearchClient client = new ElasticsearchClient();
  }

  @Data
  @ToString
  @EqualsAndHashCode
  private static class Indexes {
    private FileCentricIndex fileCentric = new FileCentricIndex();
    private AnalysisCentricIndex analysisCentric = new AnalysisCentricIndex();
  }

  @Data
  @ToString
  @EqualsAndHashCode
  private static class FileCentricIndex {
    private String name = "file_centric";
    private String alias = "file_centric";
    private boolean enabled = true;
  }

  @Data
  @ToString
  @EqualsAndHashCode
  private static class AnalysisCentricIndex {
    private String name = "analysis_centric";
    private String alias = "analysis_centric";
    private boolean enabled = true;
  }

  @Data
  @ToString
  @EqualsAndHashCode
  private static class ElasticsearchClient {
    private ElasticsearchAuth basicAuth = new ElasticsearchAuth();
    private boolean trustSelfSignedCert = false;
    private int docsPerBulkReqMax = 1000;
    private int connectionTimeout = 5000;
    private int socketTimeout = 10000;
    private ElasticsearchClientRetry retry = new ElasticsearchClientRetry();

    @Data
    @ToString
    @EqualsAndHashCode
    private static class ElasticsearchClientRetry {
      private int maxAttempts = 3;
      private int waitDurationMillis = 100;
    }

    @Data
    @ToString
    @EqualsAndHashCode
    private static class ElasticsearchAuth {
      private boolean enabled = false;
      private String user;
      private String password;
    }
  }

  @Data
  @ToString
  @EqualsAndHashCode
  private static class Notifications {
    private SlackConfig slack = new SlackConfig();

    @Data
    @ToString
    @EqualsAndHashCode
    private static class SlackConfig {
      private boolean enabled = false;
      private String url;
      private String channel;
      private String username;
      private Templates templates = new Templates();
      private Set<String> notifiedOn = Set.of(NotificationName.ALL.name().toUpperCase());
      private int maxDataLength = 1000;

      @Data
      @ToString
      @EqualsAndHashCode
      private static class Templates {
        private static String DEFAULT_TEMPLATE = "##TYPE## ##DATA##";
        private String error = DEFAULT_TEMPLATE;
        private String warning = DEFAULT_TEMPLATE;
        private String info = DEFAULT_TEMPLATE;
      }
    }
  }
}
