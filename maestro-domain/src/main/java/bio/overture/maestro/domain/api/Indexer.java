package bio.overture.maestro.domain.api;

import bio.overture.maestro.domain.api.message.IndexResult;
import bio.overture.maestro.domain.api.message.IndexStudyCommand;
import bio.overture.maestro.domain.api.message.IndexStudyRepositoryCommand;
import bio.overture.maestro.domain.port.outbound.indexing.FileCentricIndexAdapter;
import lombok.NonNull;
import reactor.core.publisher.Mono;

/**
 * Main entry point for the Indexer API
 */
public interface Indexer {
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
    Mono<IndexResult> indexStudyRepository(@NonNull IndexStudyRepositoryCommand indexStudyRepositoryCommand);
}
