import type { KafkaMessage } from 'kafkajs';

import {
	type DataRecordValue,
	type ElasticsearchService,
	logger,
	type LyricRepositoryConfig,
	type RepositoryIndexingOperations,
	type SongRepositoryConfig,
} from '@overture-stack/maestro-common';

import { getRepoInformation } from '../../repository/index.js';
import { parseMessage } from './parser.js';

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
 * Process a Request Message based on the type of the request, by checking whether the payload corresponds to
 * an analysis request, a study message, or a repository message, and performs the appropriate action,
 * such as deleting or indexing data.
 * If the message format is invalid, it throws an error.
 * @param repositories
 * @param message
 * @param indexer
 * @param repositoryIndexingApi
 * @returns
 */
export const processRequestMessage = async ({
	repositories,
	message,
	indexer,
	repositoryIndexingApi,
}: {
	repositories: (SongRepositoryConfig | LyricRepositoryConfig)[];
	message: KafkaMessage;
	indexer: ElasticsearchService;
	repositoryIndexingApi: RepositoryIndexingOperations;
}) => {
	const parsedPayload = parseMessage(message.value);
	if (!parsedPayload) {
		throw new Error('Failed to parse message: Invalid format');
	}
	const repositoryCode = parsedPayload.repositoryCode?.toString();
	if (!repositoryCode) {
		throw new Error('Missing repositoryCode in the message payload');
	}
	const repoInfo = getRepoInformation(repositories, repositoryCode);
	if (!repoInfo) {
		throw new Error(`Unknown repositoryCode: '${repositoryCode}' is not recognized.`);
	}

	logger.info(`Processing a Kafka message for request indexing in repository '${repoInfo.code}'`);

	if (isAnalysisRequest(parsedPayload)) {
		if (parsedPayload.remove) {
			if (parsedPayload.analysisId) {
				// Delete a document only when 'remove' is true and  'analysisId' is present
				indexer.deleteData(repoInfo.indexName, parsedPayload.analysisId.toString());
			} else {
				const message = `Remove document message is missing the required analysis ID`;
				throw new Error(message);
			}
		} else {
			// Fetch a single Document and Index it using api
			repositoryIndexingApi.indexRecord(
				parsedPayload.repositoryCode,
				parsedPayload.studyId.toString(),
				parsedPayload.analysisId.toString(),
			);
		}
	} else if (isStudyRequest(parsedPayload)) {
		// Fetch all Documents in a Study and Index them using the api
		repositoryIndexingApi.indexOrganization(parsedPayload.repositoryCode, parsedPayload.studyId);
	} else if (isRepoRequest(parsedPayload)) {
		// Fetch all Documents in a repository and Index them using the api
		repositoryIndexingApi.indexRepository(parsedPayload.repositoryCode);
	} else {
		const message = `Invalid message format: does not match any known request type`;
		throw new Error(message);
	}
};
