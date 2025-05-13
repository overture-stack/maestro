import {
	type DataRecordValue,
	type ElasticsearchService,
	logger,
	type SongRepositoryConfig,
} from '@overture-stack/maestro-common';

/**
 * Handles a Song Document Message and processes it based on the state of the document.
 * It checks if the document is in an indexable state (e.g. `PUBLISHED`). If so, it indexes the document.
 * If the document is in a non-indexable state (e.g. `UNPUBLISHED`), it deletes the document from the index.
 * If the message format is invalid, it throws an error.
 * @param repository
 * @param payload
 * @param indexer
 */
export const handleSongDocumentMessage = async (
	repository: SongRepositoryConfig,
	payload: Record<string, DataRecordValue>,
	indexer: ElasticsearchService,
) => {
	const indexName = repository.indexName;
	const indexableStates = repository.indexableStudyStates.split(',').map((state) => state.trim());
	if (indexableStates.length === 0 || (payload.state && indexableStates.includes(payload.state.toString()))) {
		// Only index the analysis when it has an indexable state (e.g. PUBLISHED)
		// Map 'analysisId' to the Elasticsearch '_id' field to ensure document uniqueness
		payload._id = payload.analysisId;
		await indexer.bulkUpsert(indexName, [payload]);
	} else if (payload?.analysisId) {
		// if the state is not an indexable state (e.g. UNPUBLISHED), remove the document from the index
		await indexer.deleteData(indexName, payload.analysisId.toString());
	} else {
		const message = `Invalid message format: ${JSON.stringify(payload)}`;
		logger.error(message);
		throw new Error(message);
	}
};
