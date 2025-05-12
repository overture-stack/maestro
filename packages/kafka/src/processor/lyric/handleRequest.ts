import {
	type DataRecordValue,
	type ElasticsearchService,
	logger,
	type LyricRepositoryConfig,
	type RepositoryIndexingOperations,
} from '@overture-stack/maestro-common';

interface DocumentRequestMessage extends Record<string, DataRecordValue> {
	systemId: string;
	organization: string;
	repositoryCode: string;
}

interface OrganizationRequestMessage extends Record<string, DataRecordValue> {
	organization: string;
	repositoryCode: string;
}

interface RepositoryRequestMessage extends Record<string, DataRecordValue> {
	repositoryCode: string;
}

/**
 * Determine if the given message is an `DocumentRequestMessage`
 * It checks if the message contains valid `systemId`, `organization`, and `repositoryCode` properties.
 * @param message
 * @returns
 */
export const isDocumentRequest = (message: Record<string, DataRecordValue>): message is DocumentRequestMessage => {
	return !!message.systemId && !!message.organization && !!message.repositoryCode;
};

/**
 * Determine if the given message is a `OrganizationRequestMessage`.
 * It checks if the message has a valid `organization` and `repositoryCode`, and ensures that `systemId` is not present.
 * @param message
 * @returns
 */
export const isOrganizationRequest = (
	message: Record<string, DataRecordValue>,
): message is OrganizationRequestMessage => {
	return !message.systemId && !!message.organization && !!message.repositoryCode;
};

/**
 * Determine if the given message is a `RepositoryRequestMessage`.
 * It checks if the message has a valid `repositoryCode` and ensures that neither `systemId` nor `organization` are present.
 * @param message
 * @returns
 */
export const isRepositoryRequest = (message: Record<string, DataRecordValue>): message is RepositoryRequestMessage => {
	return !message.systemId && !message.organization && !!message.repositoryCode;
};

/**
 * Handles a Lyric Request Message and processes it based on the type of the request.
 * It checks whether the payload corresponds to a single Document request, an organization message, or a repository message,
 * and performs the appropriate action, such as deleting or fetching the data to index
 * If the message format is invalid, it throws an error.
 * @param repository
 * @param payload
 * @param indexer
 * @param repositoryIndexingApi
 */
export const handleLyricRequestMessage = async (
	repository: LyricRepositoryConfig,
	payload: Record<string, DataRecordValue>,
	indexer: ElasticsearchService,
	repositoryIndexingApi: RepositoryIndexingOperations,
) => {
	if (isDocumentRequest(payload)) {
		if (payload.remove) {
			if (payload.systemId) {
				// Delete a document only when 'remove' is true and  'systemId' is present
				indexer.deleteData(repository.indexName, payload.systemId.toString());
			} else {
				const message = `Invalid message format: ${JSON.stringify(payload)}`;
				logger.error(message);
				throw new Error(message);
			}
		} else {
			// Fetch a single Document and Index it using api
			repositoryIndexingApi.indexRecord(
				payload.repositoryCode,
				payload.organization.toString(),
				payload.systemId.toString(),
			);
		}
	} else if (isOrganizationRequest(payload)) {
		// Fetch all Documents in an Organization and Index them using the api
		repositoryIndexingApi.indexOrganization(payload.repositoryCode, payload.organization);
	} else if (isRepositoryRequest(payload)) {
		// Fetch all Documents in a Category and Index them using the api
		repositoryIndexingApi.indexRepository(payload.repositoryCode);
	} else {
		const message = `Invalid message format: ${JSON.stringify(payload)}`;
		logger.error(message);
		throw new Error(message);
	}
};
