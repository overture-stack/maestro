package bio.overture.maestro.domain.port.outbound;


import bio.overture.maestro.domain.api.message.IndexResult;
import bio.overture.maestro.domain.port.outbound.message.BatchIndexFilesCommand;
import reactor.core.publisher.Mono;

/**
 * Adapter for the indexing server client, this provides the indexer with needed APIs
 * to index documents in the index server
 */
public interface FileDocumentIndexingAdapter {
    /**
     * Indexes a batch of fileDocuments in one call
     *
     * @param batchIndexFilesCommand contains the list of files to index
     * @return Result indicating whether the operation succeeded or not.
     */
    Mono<IndexResult> batchIndexFiles(BatchIndexFilesCommand batchIndexFilesCommand);
}
