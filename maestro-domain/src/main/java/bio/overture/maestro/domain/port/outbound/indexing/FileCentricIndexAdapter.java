package bio.overture.maestro.domain.port.outbound.indexing;


import bio.overture.maestro.domain.api.exception.IndexerException;
import bio.overture.maestro.domain.api.message.IndexResult;
import bio.overture.maestro.domain.entities.indexing.FileCentricDocument;
import lombok.NonNull;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

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
     * Returns a map of the ids and the corresponding files, it's a map for performance optimization
     * so that the Indexer can get quicker access since this method is needed for conflict detection
     * and the main usage will be to check if a file exists or not on elastic search.
     *
     * @param ids a list of ids to fetch documents by from elastic search.
     * @return map contains each id and the found document.
     */
    Mono<Map<String, FileCentricDocument>> fetchByIds(List<String> ids);

    /**
     * Method to delete file documents from the file centric index
     *
     * @param fileCentricDocuments the list of file to delete
     * @return indexer exception instance contains the list of failures.
     */
    Mono<IndexerException> removeFiles(List<FileCentricDocument> fileCentricDocuments);
}
