import {
	BadRequest,
	type IElasticsearchService,
	type IndexResult,
	InternalServerError,
	isEmpty,
	logger,
	type MaestroProviderConfig,
	type RepositoryIndexingOperations,
} from '@overture-stack/maestro-common';
import { getRepoInformation, repository } from '@overture-stack/maestro-repository';

/**
 * Accumulates IndexResults objects, combining their failure data and determining overall success
 * @param accumulator The initial `IndexResult` object that serves as the base for merging.
 * @param result The `IndexResult` object to merge with the accumulator
 * @returns
 */
const mergeResult = (accumulator: IndexResult, result: IndexResult): IndexResult => {
	return {
		indexName: result.indexName,
		failureData: { ...accumulator.failureData, ...result.failureData },
		successful: Object.keys(accumulator.failureData).length === 0 && Object.keys(result.failureData).length === 0,
	};
};

/**
 * Creates an object containing indexing operations to be used in the API
 * @param config The configuration object for the `MaestroProvider`, which includes repository information
 * @param indexer An implementation of `IElasticsearchService` used for performing Elasticsearch operations
 * @returns
 */
export const api = (config: MaestroProviderConfig, indexer: IElasticsearchService): RepositoryIndexingOperations => {
	const repositories = config.repositories;
	if (!repositories) {
		return {
			indexOrganization: () => {
				throw new InternalServerError(`Invalid repository configuration`);
			},
			indexRecord: () => {
				throw new InternalServerError(`Invalid repository configuration`);
			},
			indexRepository: () => {
				throw new InternalServerError(`Invalid repository configuration`);
			},
			removeIndexRecord: () => {
				throw new InternalServerError(`Invalid repository configuration`);
			},
		};
	}

	/**
	 * Performs asynchronous fetch and indexing operations for a specified repository
	 * @param repoCode
	 * @returns
	 */
	const indexRepository = async (repoCode: string): Promise<IndexResult> => {
		const repoInfo = getRepoInformation(repositories, repoCode);

		if (!repoInfo) {
			logger.error(`Invalid repository information for repository code '${repoCode}'`);
			throw new BadRequest(`Invalid repository code '${repoCode}'`);
		}

		const resultIndex: IndexResult = {
			indexName: repoInfo.indexName,
			successful: true,
			failureData: {},
		};

		for await (const items of repository(repoInfo).getRepositoryRecords()) {
			const result = await indexer.bulkUpsert(repoInfo.indexName, items);
			mergeResult(resultIndex, result);
		}
		return resultIndex;
	};

	/**
	 * Performs asynchronous fetch and indexing operations for a specified organization
	 * @param repoCode
	 * @param organization
	 * @returns
	 */
	const indexOrganization = async (repoCode: string, organization: string): Promise<IndexResult> => {
		const repoInfo = getRepoInformation(repositories, repoCode);

		if (!repoInfo) {
			logger.error(`Invalid repository information for repository code '${repoCode}'`);
			throw new BadRequest(`Invalid repository code '${repoCode}'`);
		}

		const resultIndex: IndexResult = {
			indexName: repoInfo.indexName,
			successful: true,
			failureData: {},
		};

		for await (const items of repository(repoInfo).getOrganizationRecords({ organization })) {
			const result = await indexer.bulkUpsert(repoInfo.indexName, items);
			mergeResult(resultIndex, result);
		}
		return resultIndex;
	};

	/**
	 * Performs asynchronous fetch and indexing operation for a specified record
	 * @param repoCode
	 * @param organization
	 * @param recordId
	 * @returns
	 */
	const indexRecord = async (repoCode: string, organization: string, recordId: string): Promise<IndexResult> => {
		const repoInfo = getRepoInformation(repositories, repoCode);

		if (!repoInfo) {
			logger.error(`Invalid repository information for repository code '${repoCode}'`);
			throw new BadRequest(`Invalid repository code '${repoCode}'`);
		}

		// Fetch the record within a repository
		const repoRecord = await repository(repoInfo).getRecord({ organization, id: recordId });

		if (!isEmpty(repoRecord)) {
			// Index records using batchUpsert
			return indexer.addData(repoInfo.indexName, repoRecord);
		} else {
			logger.error(`Record '${recordId}' not found in organization '${organization}'`);
			throw new BadRequest(`Record '${recordId}' not found in organization '${organization}'`);
		}
	};

	/**
	 * Performs asynchronous fetch and remove indexing for a specified record
	 * @param repoCode
	 * @param organization
	 * @param recordId
	 * @returns
	 */
	const removeIndexRecord = async (repoCode: string, organization: string, recordId: string): Promise<IndexResult> => {
		const repoInfo = getRepoInformation(repositories, repoCode);

		if (!repoInfo) {
			logger.error(`Invalid repository information for repository code '${repoCode}'`);
			throw new BadRequest(`Invalid repository code '${repoCode}'`);
		}

		// Fetch the record within a repository
		const repoRecord = await repository(repoInfo).getRecord({ organization, id: recordId });

		if (!isEmpty(repoRecord)) {
			return indexer.deleteData(repoInfo.indexName, recordId);
		} else {
			logger.error(`Record '${recordId}' not found in organization '${organization}'`);
			throw new BadRequest(`Record '${recordId}' not found in organization '${organization}'`);
		}
	};

	return {
		indexOrganization,
		indexRecord,
		indexRepository,
		removeIndexRecord,
	};
};
