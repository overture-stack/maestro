package bio.overture.maestro.app.infra.adapter.inbound.webapi;

import bio.overture.maestro.domain.api.Indexer;
import bio.overture.maestro.domain.api.message.IndexResult;
import bio.overture.maestro.domain.api.message.IndexStudyCommand;
import bio.overture.maestro.domain.api.message.IndexStudyRepositoryCommand;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
public class ManagementController {

    private final Indexer indexer;

    @Inject
    public ManagementController(Indexer indexer) {
        this.indexer = indexer;
    }

    @GetMapping
    public Mono<Map<String, String>> ping() {
        val response = new HashMap<String, String>();
        response.put("status", "up");
        return Mono.just(response);
    }

    @PostMapping("/index/repository/{repositoryCode}/study/{studyId}")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<IndexResult> indexStudy(@PathVariable String studyId, @PathVariable String repositoryCode) {
        log.debug("in indexStudy, args studyId {}, repoId: {}", studyId, repositoryCode);
        return indexer.indexStudy(IndexStudyCommand.builder()
                .repositoryCode(repositoryCode)
                .studyId(studyId)
                .build()
        );
    }

    @PostMapping("/index/repository/{repositoryCode}")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<IndexResult> indexRepository(@PathVariable String repositoryCode) {
        return indexer.indexStudyRepository(IndexStudyRepositoryCommand.builder()
            .repositoryCode(repositoryCode)
            .build()
        );
    }

}
