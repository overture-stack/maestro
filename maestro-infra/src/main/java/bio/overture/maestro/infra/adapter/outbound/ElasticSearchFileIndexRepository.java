package bio.overture.maestro.infra.adapter.outbound;

import bio.overture.maestro.domain.message.IndexResult;
import bio.overture.maestro.domain.message.out.FileDocument;
import bio.overture.maestro.domain.port.outbound.FileIndexRepository;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

public class ElasticSearchFileIndexRepository implements FileIndexRepository {

    private final ElasticsearchTemplate template;

    @Inject
    public ElasticSearchFileIndexRepository(ElasticsearchTemplate template) {
        this.template = template;
    }

    @Override
    public Mono<IndexResult> batchIndexFiles(List<FileDocument> fileList) {
        return  Mono.fromSupplier(() -> this.bulkIndexFiles(fileList))
                // https://stackoverflow.com/questions/52071249/how-to-wrap-a-flux-with-a-blocking-operation-in-the-subscribe
                .publishOn(Schedulers.elastic())
                .onErrorReturn((e) -> e instanceof ElasticsearchException, IndexResult.builder().successful(false).build());
    }

    private IndexResult bulkIndexFiles(List<FileDocument> filesList) {
        template.bulkIndex(filesList.stream()
                .map(file -> new IndexQueryBuilder()
                        .withId(file.getId())
                        .withIndexName("files")
                        .withObject(file)
                        .build()
                ).collect(Collectors.toList()));
        return IndexResult.builder().successful(true).build();
    }
}
