package bio.overture.maestro.infra.adapter.outbound;

import bio.overture.maestro.domain.api.exception.UpstreamServiceException;
import bio.overture.maestro.domain.api.message.IndexResult;
import bio.overture.maestro.domain.entities.indexer.FileCentricDocument;
import bio.overture.maestro.domain.port.outbound.FileDocumentIndexingAdapter;
import bio.overture.maestro.domain.port.outbound.message.BatchIndexFilesCommand;
import bio.overture.maestro.infra.config.ApplicationProperties;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

import static bio.overture.maestro.domain.utility.StringUtilities.inputStreamToString;

@Slf4j
public class FileCentricElasticSearchAdapter implements FileDocumentIndexingAdapter {

    private final ElasticsearchRestTemplate template;
    private final Resource indexSettings;
    private final ResourceLoader resourceLoader;
    private String alias;

    @Inject
    public FileCentricElasticSearchAdapter(ElasticsearchRestTemplate template,
                                           ResourceLoader resourceLoader,
                                           ApplicationProperties properties) {
        this.template = template;
        this.alias = properties.getFileCentricAlias();
        this.indexSettings = properties.getIndexSettings();
        this.resourceLoader = resourceLoader;
    }

    @Override
    public Mono<IndexResult> batchIndexFiles(@NonNull BatchIndexFilesCommand batchIndexFilesCommand) {
        log.debug("in batchIndexFiles, args: {} ", batchIndexFilesCommand.getFiles().size());
        return  Mono.fromSupplier(() -> this.bulkIndexFiles(batchIndexFilesCommand.getFiles()))
            .onErrorMap((e) -> e instanceof ElasticsearchException,
                (e) -> new UpstreamServiceException("batch Index failed", e))
            .subscribeOn(Schedulers.elastic());
    }

    @Retryable(
        maxAttempts = 5,
        backoff = @Backoff(value = 1000, multiplier=1.5)
    )
    public void initialize() {
        val indexExists = this.template.indexExists(this.alias);
        log.info("indexExists result: {} ", indexExists);
        if (!indexExists) {
            this.createIndex();
            template.putMapping(this.alias, this.alias, loadMappingMap(this.alias));
            log.info("index {} with mapping {} have been created", this.alias, this.alias);
        }
    }

    @Recover
    public void recover(Throwable t) {
        log.error("couldn't initialize the index");
    }

    /* *******************
     *  Private methods
     *********************/
    @SneakyThrows
    private void createIndex() {
        val indexSettings = inputStreamToString(this.indexSettings.getInputStream());
        this.template.createIndex(this.alias, indexSettings);
    }

    private IndexResult bulkIndexFiles(List<FileCentricDocument> filesList) {
        log.trace("in bulkIndexFiles, filesList count : {} ", filesList.size());
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
        log.trace("in loadMappingMap: {}", typeName);
        val mapping = this.resourceLoader.getResource("classpath:" + typeName + ".mapping.json");
        return inputStreamToString(mapping.getInputStream());
    }

}
