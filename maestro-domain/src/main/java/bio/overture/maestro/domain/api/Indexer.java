package bio.overture.maestro.domain.api;

import bio.overture.maestro.domain.message.IndexResult;
import bio.overture.maestro.domain.message.IndexStudyCommand;
import reactor.core.publisher.Mono;

public interface Indexer {
    Mono<IndexResult> indexStudy(IndexStudyCommand indexStudyCommand);
}
