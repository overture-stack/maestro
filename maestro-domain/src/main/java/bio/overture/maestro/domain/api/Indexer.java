package bio.overture.maestro.domain.api;

import bio.overture.maestro.domain.api.message.IndexResult;
import bio.overture.maestro.domain.api.message.IndexStudyCommand;
import reactor.core.publisher.Mono;

/**
 * Main entry point for the Indexer apis
 */
public interface Indexer {
    Mono<IndexResult> indexStudy(IndexStudyCommand indexStudyCommand);
    void indexAll();
}
