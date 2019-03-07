package bio.overture.maestro.infra.adapter.inbound;

import bio.overture.maestro.domain.api.Indexer;
import bio.overture.maestro.domain.message.IndexResult;
import bio.overture.maestro.domain.message.IndexStudyCommand;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.inject.Inject;

@RestController
public class ManagementController {

    private final Indexer indexer;

    @Inject
    public ManagementController(Indexer indexer) {
        this.indexer = indexer;
    }

    @PostMapping("/index/{repositoryId}/{studyId}")
    public Mono<IndexResult> indexStudy(String studyId) {
        return indexer.indexStudy(IndexStudyCommand.builder()
                .repositoryCode("collab")
                .studyId(studyId)
                .build()
        );
    }

}
