package bio.overture.maestro.infra.adapter.outbound;

import bio.overture.maestro.domain.api.exception.UpstreamServiceException;
import bio.overture.maestro.domain.api.message.IndexResult;
import bio.overture.maestro.domain.entities.indexer.FileCentricDocument;
import bio.overture.maestro.domain.port.outbound.FileDocumentIndexServerAdapter;
import bio.overture.maestro.domain.port.outbound.message.BatchIndexFilesCommand;
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

import static bio.overture.maestro.domain.utility.StringUtilities.inputStreamToString;

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
    public Mono<IndexResult> batchIndexFiles(BatchIndexFilesCommand batchIndexFilesCommand) {
        return  Mono.fromSupplier(() -> this.bulkIndexFiles(batchIndexFilesCommand.getFiles()))
            .onErrorMap((e) -> e instanceof ElasticsearchException, (e) -> new UpstreamServiceException("batch Index failed", e))
            .subscribeOn(Schedulers.elastic());
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
            template.putMapping(this.alias, this.alias, loadMappingMap(this.alias));
        }
    }

    @SneakyThrows
    private void createIndex() {
        val indexSettings = inputStreamToString(this.indexSettings.getInputStream());
        this.template.createIndex(this.alias, indexSettings);
    }

    private IndexResult bulkIndexFiles(List<FileCentricDocument> filesList) {
        this.template.bulkIndex(filesList.stream()
            .map(file -> new IndexQueryBuilder()
                .withId(file.getObjectId())
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
        return inputStreamToString(mapping.getInputStream());
    }

}
