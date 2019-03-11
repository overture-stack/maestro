package bio.overture.maestro.domain.api;

import bio.overture.maestro.domain.message.in.IndexResult;
import bio.overture.maestro.domain.message.in.IndexStudyCommand;
import reactor.core.publisher.Mono;

public interface Indexer {
    Mono<IndexResult> indexStudy(IndexStudyCommand indexStudyCommand);
    void indexAll();
}
