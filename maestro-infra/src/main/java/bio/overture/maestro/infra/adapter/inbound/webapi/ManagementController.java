package bio.overture.maestro.infra.adapter.inbound.webapi;

import bio.overture.maestro.domain.api.Indexer;
import bio.overture.maestro.domain.api.message.IndexResult;
import bio.overture.maestro.domain.api.message.IndexStudyCommand;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

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

    @PostMapping("/index/repository/{repositoryId}/study/{studyId}")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<IndexResult> indexStudy(@PathVariable String studyId, @PathVariable String repositoryId) {
        return indexer.indexStudy(IndexStudyCommand.builder()
                .repositoryCode(repositoryId)
                .studyId(studyId)
                .build()
        );
    }

    @PostMapping("/index/repository/{repositoryId}")
    @ResponseStatus(HttpStatus.CREATED)
    public void indexRepository(@PathVariable String repositoryId) {
        indexer.indexAll();
    }


}
