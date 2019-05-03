package bio.overture.maestro.domain.port.outbound.indexing;


import bio.overture.maestro.domain.api.message.IndexResult;
import bio.overture.maestro.domain.entities.indexing.FileCentricDocument;
import lombok.NonNull;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

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
     * Batch fetch documents from the index by the specified ids.
     * @param ids a list of ids to fetch documents by from elastic search.
     * @return List contains found documents.
     */
    Mono<List<FileCentricDocument>> fetchByIds(List<String> ids);

    /**
     * Method to delete file documents from the file centric index
     *
     * @param fileCentricDocumentIds the list of file to delete
     * @return indexer exception instance contains the list of failures.
     */
    Mono<Void> removeFiles(Set<String> fileCentricDocumentIds);
}
