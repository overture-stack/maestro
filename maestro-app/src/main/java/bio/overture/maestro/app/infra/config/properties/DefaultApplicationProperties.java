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

    @Value("${maestro.elasticsearch.cluster-nodes}")
    private List<String> hosts;

    @Value("${maestro.elasticsearch.indexes.file-centric.alias:file-centric}")
    private String fileCentricAlias;

    @Value("${maestro.elasticsearch.client.docs-per-bulk-req-max:1000}")
    private Integer docsPerBulkReqMax;

    @Value("${maestro.song.max-retries:3}")
    private Integer songMaxRetries;

    @Value("${maestro.song.timeout-sec:10}")
    private Integer songTimeoutSeconds;

    @Value("classpath:index.settings.json")
    private Resource indexSettings;

    @Value("classpath:file_centric.mapping.json")
    private Resource fileCentricMapping;

    @Value("classpath:${maestro.exclusion-rules.file-name:exclusion-rules.yml}")
    private Resource exclusionRules;

    @Value("${maestro.elasticsearch.client.connection-timeout:5000}")
    private long elasticSearchClientConnectionTimeout;

    @Value("${maestro.elasticsearch.client.socket-timeout:10000}")
    private long elasticSearchClientSocketTimeout;

    @Value("${maestro.elasticsearch.client.retry.max-attempts:3}")
    private int elasticSearchRetryMaxAttempts;

    @Value("${maestro.elasticsearch.client.retry.wait-duration-millis:100}")
    private long elasticSearchRetryWaitDurationMillis;

    private List<DefaultPropertiesFileRepository> repositories;

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
    public Resource indexSettings() {
        return this.indexSettings;
    }

    @Override
    public long elasticSearchClientConnectionTimeoutMillis() {
        return this.elasticSearchClientConnectionTimeout;
    }

    @Override
    public long elasticSearchClientSocketTimeoutMillis() {
        return this.elasticSearchClientSocketTimeout;
    }

    @Override
    public List<PropertiesFileRepository> repositories() {
        return List.copyOf(this.repositories);
    }

    @Override
    public Resource fileCentricMapping() {
        return fileCentricMapping;
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
    public int songTimeoutSeconds() {
        return songTimeoutSeconds;
    }

    @Override
    public long elasticSearchRetryWaitDurationMillis() {
        return elasticSearchRetryWaitDurationMillis;
    }

    @Override
    public int elasticSearchRetryMaxAttempts() {
        return elasticSearchRetryMaxAttempts;
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