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
    long elasticSearchClientConnectionTimeout();
    long elasticSearchClientSocketTimeout();
    List<PropertiesFileRepository> repositories();
}
