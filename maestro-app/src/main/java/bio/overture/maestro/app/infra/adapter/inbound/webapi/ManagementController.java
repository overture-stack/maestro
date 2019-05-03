package bio.overture.maestro.app.infra.adapter.inbound.webapi;

import bio.overture.maestro.domain.api.Indexer;
import bio.overture.maestro.domain.api.message.*;
import bio.overture.maestro.domain.entities.indexing.rules.ExclusionRule;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
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

    @PostMapping("/index/repository/{repositoryCode}/study/{studyId}/analysis/{analysisId}")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<IndexResult> indexAnalysis(@PathVariable String analysisId, @PathVariable String studyId, @PathVariable String repositoryCode) {
        log.debug("in indexAnalysis, args studyId {}, repoId: {}, analysisId : {}", studyId, repositoryCode, analysisId);
        return indexer.indexAnalysis(IndexAnalysisCommand.builder()
            .analysisIdentifier(
                AnalysisIdentifier.builder()
                    .repositoryCode(repositoryCode)
                    .analysisId(analysisId)
                    .studyId(studyId)
                    .build()
            )
            .build()
        );
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

    @GetMapping("/rules/")
    public List<? extends ExclusionRule> getRules() {
        return indexer.getAllRules();
    }

    @PostMapping("/rules/byId/{type}")
    public void addExclusionRule(@RequestBody List<String> ids) {
        indexer.addRule(null);
    }

    @DeleteMapping("/rules/byId/{type}")
    public void deleteExclusionRule(@RequestParam List<String> ids) {
        indexer.deleteRule(null);
    }
}
