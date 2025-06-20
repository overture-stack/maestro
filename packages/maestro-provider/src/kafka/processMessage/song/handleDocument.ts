import {
	convertAnalyses,
	type DataRecordNested,
	type ElasticsearchService,
	logger,
	type SongRepositoryConfig,
} from '@overture-stack/maestro-common';

import { isArrayOfObjects } from '../../../repository/utils/utils.js';

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
	payload: DataRecordNested,
	indexer: ElasticsearchService,
) => {
	const indexName = repository.indexName;
	const indexableStates = repository.indexableStudyStates;

	// Only index the analysis when it has an indexable state (e.g. PUBLISHED)
	if (
		indexableStates.length === 0 ||
		(payload.state && indexableStates.some((value) => String(value) === String(payload.state)))
	) {
		// Example payload — see docs/usage.md for full details:
		// {
		// 	"analysisId": "3bb8a1ff-ca21-4132-96b4-60cad182db06",
		// 	"studyId": "TEST-CA",
		// 	"songServerId": "collab",
		// 	"state": "PUBLISHED",
		// 	"analysis": {
		// 		"analysisId": "3bb8a1ff-ca21-4132-96b4-60cad182db06",
		// 		"analysisState": "PUBLISHED",
		// 		"files": [
		// 			{
		// 				"objectId": "a74f4e10-f648-4c6d-ac14-0dc7dfdcd6a0",
		// 				"fileName": "TEST-CA.fasta",
		// 				"fileSize": 29937,
		// 				"fileType": "FASTA",
		// 				"fileMd5sum": "0000000000",
		// 				"fileAccess": "open",
		// 				"dataType": "FASTA"
		// 			}
		// 		]
		// 	}
		// }
		const { analysis } = payload;
		const arrayAnalysis = Array.isArray(analysis) ? analysis : [analysis];

		if (!isArrayOfObjects(arrayAnalysis)) {
			const message = `Invalid message format: ${JSON.stringify(payload)}`;
			logger.error(message);
			throw new Error(message);
		}
		const converted = convertAnalyses(repository, arrayAnalysis);
		return indexer.bulkUpsert(indexName, converted);
	} else if (payload?.analysisId) {
		// if the state is not an indexable state (e.g. UNPUBLISHED), remove the document from the index
		await indexer.deleteData(indexName, payload.analysisId.toString());
	} else {
		const message = `Invalid message format: ${JSON.stringify(payload)}`;
		logger.error(message);
		throw new Error(message);
	}
};
