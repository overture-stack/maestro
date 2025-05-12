import {
	type DataRecordValue,
	type ElasticsearchService,
	logger,
	type LyricRepositoryConfig,
} from '@overture-stack/maestro-common';

/**
 * Handles a Lyric Document Message and processes it based on the validity of the document.
 * Index every document if 'validDataOnly' is false, or if 'validDataOnly' is true and the payload is marked as valid
 * Otherwise, the document should be deleted when the systemId is present.
 * If the message format is invalid, it throws an error.
 * @param repository
 * @param payload
 * @param indexer
 */
export const handleLyricDocumentMessage = async (
	repository: LyricRepositoryConfig,
	payload: Record<string, DataRecordValue>,
	indexer: ElasticsearchService,
) => {
	if (!repository.validDataOnly || (repository.validDataOnly && payload.isValid)) {
		// Index every document if 'validDataOnly' is false, or
		// if 'validDataOnly' is true and the payload is marked as valid ('isValid' is true)
		await indexer.bulkUpsert(repository.indexName, [payload]);
	} else if (payload.systemId) {
		// Otherwise, the document should be deleted when the systemId is present.
		await indexer.deleteData(repository.indexName, payload.systemId.toString());
	} else {
		const message = `Invalid message format: ${JSON.stringify(payload)}`;
		logger.error(message);
		throw new Error(message);
	}
};
