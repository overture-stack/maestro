package bio.overture.maestro.infra.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Getter
public class ApplicationProperties {

    public final static String MAESTRO_PREFIX = "maestro";
    public final static String ELASTIC_SEARCH_CLUSTER_NODES =  "maestro.elasticsearch.cluster-nodes";

    @Value("${maestro.elasticsearch.cluster-nodes}")
    private List<String> hosts;

    @Value("${maestro.elasticsearch.indexes.file-centric.alias:file-centric}")
    private String fileCentricAlias;

    @Value("classpath:index.settings.json")
    private Resource indexSettings;
}