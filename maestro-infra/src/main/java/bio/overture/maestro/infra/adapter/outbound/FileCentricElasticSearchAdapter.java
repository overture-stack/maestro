package bio.overture.maestro.infra.adapter.outbound;

import bio.overture.maestro.domain.message.in.IndexResult;
import bio.overture.maestro.domain.entities.FileCentricDocument;
import bio.overture.maestro.domain.port.outbound.FileDocumentIndexServerAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;


import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@ConfigurationProperties(prefix = "maestro.elasticsearch.indexes.file-centric")
public class FileCentricElasticSearchAdapter implements FileDocumentIndexServerAdapter {

    private final ElasticsearchTemplate template;
    private final Resource indexSettings;
    private final ResourceLoader resourceLoader;

    @Setter
    private String alias;

    @Inject
    public FileCentricElasticSearchAdapter(ElasticsearchTemplate template,
                                           @Value("classpath:index.settings.json") Resource indexSettings,
                                           ResourceLoader resourceLoader) {
        this.template = template;
        this.indexSettings = indexSettings;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public Mono<IndexResult> batchIndexFiles(List<FileCentricDocument> fileList) {
        return  Mono.fromSupplier(() -> this.bulkIndexFiles(fileList))
                // https://stackoverflow.com/questions/52071249/how-to-wrap-a-flux-with-a-blocking-operation-in-the-subscribe
                .publishOn(Schedulers.elastic())
                .onErrorReturn((e) -> e instanceof ElasticsearchException, IndexResult.builder().successful(false).build());
    }

    @PostConstruct
    @SuppressWarnings("unused")
    public void postConstruct() {
        log.debug("initializing index {} ", this.alias);
        this.initialize();
        log.debug("initializing index finished {} ", this.alias);
    }

    /* *******************
     *  Private methods
     *********************/
    private void initialize() {
        val indexExists = this.template.indexExists(this.alias);
        log.debug("indexExists result: {} ", indexExists);
        if (!indexExists) {
            this.createIndex();
        }
        template.putMapping(this.alias, this.alias, loadMappingMap(this.alias));
    }

    @SneakyThrows
    private void createIndex() {
        val jsonNode = new ObjectMapper().readTree(this.indexSettings.getInputStream()).toString();
        this.template.createIndex(this.alias, jsonNode);
    }

    private IndexResult bulkIndexFiles(List<FileCentricDocument> filesList) {
        this.template.bulkIndex(filesList.stream()
                .map(file -> new IndexQueryBuilder()
                        .withId(file.getId())
                        .withIndexName(this.alias)
                        .withType(this.alias)
                        .withObject(file)
                        .build()
                ).collect(Collectors.toList())
        );
        return IndexResult.builder().successful(true).build();
    }

    @SneakyThrows
    private String loadMappingMap(String typeName) {
        val mapping = this.resourceLoader.getResource("classpath:" + typeName + ".mapping.json");
        return new ObjectMapper().readTree(mapping.getURL()).toString();
    }

}
