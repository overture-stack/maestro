import {
	type DataRecordValue,
	type ElasticsearchService,
	logger,
	type RepositoryIndexingOperations,
	type SongRepositoryConfig,
} from '@overture-stack/maestro-common';

interface AnalysisRequestMessage extends Record<string, DataRecordValue> {
	analysisId: string;
	studyId: string;
	repositoryCode: string;
}

interface StudyRequestMessage extends Record<string, DataRecordValue> {
	studyId: string;
	repositoryCode: string;
}

interface RepositoryRequestMessage extends Record<string, DataRecordValue> {
	repositoryCode: string;
}

/**
 * Determine if the given message is an `AnalysisRequestMessage`
 * It checks if the message contains valid `analysisId`, `studyId`, and `repositoryCode` properties.
 * @param message
 * @returns
 */
export const isAnalysisRequest = (message: Record<string, DataRecordValue>): message is AnalysisRequestMessage => {
	return !!message.analysisId && !!message.studyId && !!message.repositoryCode;
};

/**
 * Determine if the given message is a `StudyRequestMessage`.
 * It checks if the message has a valid `studyId` and `repositoryCode`, and ensures that `analysisId` is not present.
 * @param message
 * @returns
 */
export const isStudyRequest = (message: Record<string, DataRecordValue>): message is StudyRequestMessage => {
	return !message.analysisId && !!message.studyId && !!message.repositoryCode;
};

/**
 * Determine if the given message is a `RepositoryRequestMessage`.
 * It checks if the message has a valid `repositoryCode` and ensures that neither `analysisId` nor `studyId` are present.
 * @param message
 * @returns
 */
export const isRepoRequest = (message: Record<string, DataRecordValue>): message is RepositoryRequestMessage => {
	return !message.analysisId && !message.studyId && !!message.repositoryCode;
};

/**
 * Handles a Song Request Message and processes it based on the type of the request.
 * It checks whether the payload corresponds to an analysis request, a study message, or a repository message,
 * and performs the appropriate action, such as deleting or indexing data.
 * If the message format is invalid, it throws an error.
 * @param repository
 * @param payload
 * @param indexer
 * @param repositoryIndexingApi
 */
export const handleSongRequestMessage = async (
	repository: SongRepositoryConfig,
	payload: Record<string, DataRecordValue>,
	indexer: ElasticsearchService,
	repositoryIndexingApi: RepositoryIndexingOperations,
) => {
	if (isAnalysisRequest(payload)) {
		if (payload.removeAnalysis) {
			if (payload.analysisId) {
				// Delete a document only when 'removeAnalysis' is true and  'analysisId' is present
				indexer.deleteData(repository.indexName, payload.analysisId.toString());
			} else {
				const message = `Invalid message format: ${JSON.stringify(payload)}`;
				logger.error(message);
				throw new Error(message);
			}
		} else {
			// Fetch a single Document and Index it using api
			repositoryIndexingApi.indexRecord(
				payload.repositoryCode,
				payload.studyId.toString(),
				payload.analysisId.toString(),
			);
		}
	} else if (isStudyRequest(payload)) {
		// Fetch all Documents in a Study and Index them using the api
		repositoryIndexingApi.indexOrganization(payload.repositoryCode, payload.studyId);
	} else if (isRepoRequest(payload)) {
		// Fetch all Documents in a repository and Index them using the api
		repositoryIndexingApi.indexRepository(payload.repositoryCode);
	} else {
		const message = `Invalid message format: ${JSON.stringify(payload)}`;
		logger.error(message);
		throw new Error(message);
	}
};
