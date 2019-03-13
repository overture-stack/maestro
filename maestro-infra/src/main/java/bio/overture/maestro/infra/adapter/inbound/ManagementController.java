package bio.overture.maestro.infra.adapter.inbound;

import bio.overture.maestro.domain.api.Indexer;
import bio.overture.maestro.domain.api.message.IndexResult;
import bio.overture.maestro.domain.api.message.IndexStudyCommand;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<IndexResult> indexStudy(@PathVariable String studyId, @PathVariable String repositoryId) {
        return indexer.indexStudy(IndexStudyCommand.builder()
                .repositoryCode(repositoryId)
                .studyId(studyId)
                .build()
        );
    }

}
