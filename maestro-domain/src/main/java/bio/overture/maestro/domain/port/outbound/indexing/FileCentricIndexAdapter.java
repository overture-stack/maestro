package bio.overture.maestro.domain.port.outbound.indexing;


import bio.overture.maestro.domain.api.exception.IndexerException;
import bio.overture.maestro.domain.api.message.IndexResult;
import bio.overture.maestro.domain.entities.indexing.FileCentricDocument;
import lombok.NonNull;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Adapter for the indexing server client, this provides the indexer with needed APIs
 * to index file centric documents in the index server
 */
public interface FileCentricIndexAdapter {

    /**
     * Indexes a batch of fileDocuments in one call
     *
     * @param batchIndexFilesCommand contains the list of files to index
     * @return Result indicating whether the operation succeeded or not.
     */
    Mono<IndexResult> batchIndex(@NonNull BatchIndexFilesCommand batchIndexFilesCommand);

    /**
     * Updates a fileDocument repositories field, or indexes the whole document if doesn't exist.
     *
     * @param batchIndexFilesCommand requires the full document to insert if doesn't exist.
     * @return flag indicating if the operation was successful.
     */
    Mono<IndexResult> batchUpsertFileRepositories(@NonNull BatchIndexFilesCommand batchIndexFilesCommand);

    /**
     *
     * @param id
     * @return
     */
    Mono<List<FileCentricDocument>> fetchByIds(List<String> id);


    Mono<IndexerException> removeFiles(List<FileCentricDocument> conflictingFiles);
}
