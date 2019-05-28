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

import bio.overture.maestro.domain.entities.indexing.StorageType;
import lombok.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

import static bio.overture.maestro.app.infra.config.properties.DefaultApplicationProperties.MAESTRO_PREFIX;

/**
 * This abstracts the application from the underlaying property source
 * and allows for easier testability by mocking/proxying to this class if needed.
 */
@Component
@Setter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@ConfigurationProperties(prefix = MAESTRO_PREFIX, ignoreInvalidFields = true)
final class DefaultApplicationProperties implements ApplicationProperties {

    final static String MAESTRO_PREFIX = "maestro";

    @Value("${maestro.elasticsearch.clusterNodes}")
    private List<String> hosts;

    @Value("${maestro.elasticsearch.indexes.file_centric.alias:file_centric}")
    private String fileCentricAlias;

    @Value("${maestro.elasticsearch.client.docsPerBulkReqMax:1000}")
    private Integer docsPerBulkReqMax;

    @Value("${maestro.song.maxRetries:3}")
    private Integer songMaxRetries;

    @Value("${maestro.song.timeoutSec.study:10}")
    private Integer songStudyCallTimeout;

    @Value("${maestro.song.timeoutSec.analysis:5}")
    private int songAnalysisCallTimeoutSec;

    @Value("classpath:file_centric.json")
    private Resource fileCentricIndex;

    @Value("classpath:${maestro.exclusionRules.fileName:exclusion-rules.yml}")
    private Resource exclusionRules;

    @Value("${maestro.elasticsearch.client.connectionTimeout:5000}")
    private int elasticSearchClientConnectionTimeout;

    @Value("${maestro.elasticsearch.client.socketTimeout:10000}")
    private int elasticSearchClientSocketTimeout;

    @Value("${maestro.elasticsearch.client.retry.maxAttempts:3}")
    private int elasticSearchRetryMaxAttempts;

    @Value("${maestro.elasticsearch.client.retry.waitDurationMillis:100}")
    private long elasticSearchRetryWaitDurationMillis;

    private List<DefaultPropertiesFileRepository> repositories;

    @Value("${maestro.song.indexableStudyStatesCsv:PUBLISHED}")
    private String indexableStudyStates;

    @Override
    public List<String> elasticSearchClusterNodes() {
        return List.copyOf(this.hosts);
    }

    @Override
    public String fileCentricAlias() {
        return this.fileCentricAlias;
    }

    @Override
    public int maxDocsPerBulkRequest() {
        return this.docsPerBulkReqMax;
    }

    @Override
    public int elasticSearchClientConnectionTimeoutMillis() {
        return this.elasticSearchClientConnectionTimeout;
    }

    @Override
    public int elasticSearchClientSocketTimeoutMillis() {
        return this.elasticSearchClientSocketTimeout;
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
    public Resource exclusionRules() {
        return exclusionRules;
    }

    @Override
    public int songMaxRetries() {
        return songMaxRetries;
    }

    @Override
    public int songStudyCallTimeoutSeconds() {
        return songStudyCallTimeout;
    }

    @Override
    public long elasticSearchRetryWaitDurationMillis() {
        return elasticSearchRetryWaitDurationMillis;
    }

    @Override
    public int elasticSearchRetryMaxAttempts() {
        return elasticSearchRetryMaxAttempts;
    }

    @Override
    public String indexableStudyStatuses() {
        return indexableStudyStates;
    }

    @Override
    public int songAnalysisCallTimeoutSeconds() {
        return songAnalysisCallTimeoutSec;
    }

    @Data
    @ToString
    @EqualsAndHashCode
    private static class DefaultPropertiesFileRepository
        implements PropertiesFileRepository {

        private String name;
        private String code;
        private String url;
        private String dataPath = "/oicr.icgc/data";
        private String metadataPath = "/oicr.icgc.meta/metadata";
        private String organization;
        private String country;
        private StorageType storageType = StorageType.S3;

    }

}
