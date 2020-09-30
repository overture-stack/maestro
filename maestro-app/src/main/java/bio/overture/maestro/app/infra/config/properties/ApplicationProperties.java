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

import bio.overture.maestro.app.infra.adapter.outbound.notification.Slack;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;

/**
 * Abstraction for application properties, implementations should return deep copy to avoid mutating
 * the original properties
 */
public interface ApplicationProperties {
  String FAILURE_LOG_PROP_KEY = "maestro.failureLog.enabled";
  String MAESTRO_NOTIFICATIONS_SLACK_ENABLED_PROP_KEY = "maestro.notifications.slack.enabled";

  int webClientMaxInMemorySize();

  List<String> elasticSearchClusterNodes();

  String fileCentricAlias();

  String fileCentricIndexName();

  boolean isFileCentricIndexEnabled();

  String analysisCentricAlias();

  String analysisCentricIndexName();

  boolean isAnalysisCentricIndexEnabled();

  int maxDocsPerBulkRequest();

  int elasticSearchClientConnectionTimeoutMillis();

  int elasticSearchClientSocketTimeoutMillis();

  boolean elasticSearchTlsTrustSelfSigned();

  boolean elasticSearchBasicAuthEnabled();

  String elasticSearchAuthUser();

  String elasticSearchAuthPassword();

  List<PropertiesFileRepository> repositories();

  Resource fileCentricIndex();

  Resource analysisCentricIndex();

  Map<String, List<String>> idExclusionRules();

  int songMaxRetries();

  int songStudyCallTimeoutSeconds();

  long elasticSearchRetryWaitDurationMillis();

  int elasticSearchRetryMaxAttempts();

  String indexableStudyStatuses();

  int songAnalysisCallTimeoutSeconds();

  Slack.SlackChannelInfo getSlackChannelInfo();

  boolean disableIndexing();
}
