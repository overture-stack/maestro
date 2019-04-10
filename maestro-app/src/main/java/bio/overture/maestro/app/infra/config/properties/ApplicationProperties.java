package bio.overture.maestro.app.infra.config.properties;

import org.springframework.core.io.Resource;

import java.util.List;

/**
 * Abstraction for application properties, implementations should return deep copy to avoid mutating the original
 * properties
 */
public interface ApplicationProperties {
    List<String> elasticSearchClusterNodes();
    String fileCentricAlias();
    int maxDocsPerBulkRequest();
    Resource indexSettings();
    long elasticSearchClientConnectionTimeoutMillis();
    long elasticSearchClientSocketTimeoutMillis();
    List<PropertiesFileRepository> repositories();
    Resource fileCentricMapping();
    Resource exclusionRules();
    int songMaxRetries();
    int songTimeoutSeconds();
}
