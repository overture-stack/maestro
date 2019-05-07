package bio.overture.maestro.domain.api;

import bio.overture.maestro.domain.api.message.*;
import bio.overture.maestro.domain.entities.indexing.rules.ExclusionRule;
import bio.overture.maestro.domain.port.outbound.indexing.FileCentricIndexAdapter;
import lombok.NonNull;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Main entry point for the Indexer API
 */
public interface Indexer {

    Mono<IndexResult> indexAnalysis(@NonNull IndexAnalysisCommand indexAnalysisCommand);
    /**
     * This method will fetch the specified study from the specified repository
     * and will invoke the index server {@link FileCentricIndexAdapter}
     * adapter to batch index the resulting documents.
     *
     * @param indexStudyCommand contains the arguments needed to index a single study.
     * @return an index result indicating success or failure
     * @throws bio.overture.maestro.domain.api.exception.NotFoundException
     *          if the study or the repository are not found
     * @throws bio.overture.maestro.domain.api.exception.BadDataException
     *          if the study is empty or the structure of the study is not as expected
     *          and cannot be used to produce list of {@link bio.overture.maestro.domain.entities.indexing.FileCentricDocument}
     */
    Mono<IndexResult> indexStudy(@NonNull IndexStudyCommand indexStudyCommand);

    /**
     * This method will trigger a full indexing for a metadata repository (all studies)
     * @param indexStudyRepositoryCommand contains the repository code to index.
     * @return result indicating success/fail and failures information.
     */
    Mono<IndexResult> indexStudyRepository(@NonNull IndexStudyRepositoryCommand indexStudyRepositoryCommand);

    void addRule(AddRuleCommand addRuleCommand);
    void deleteRule(DeleteRuleCommand deleteRuleCommand);
    List<? extends ExclusionRule> getAllRules();
}
