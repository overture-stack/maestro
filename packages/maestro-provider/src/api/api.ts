import {
	type ApiResult,
	type ElasticsearchService,
	isEmpty,
	logger,
	type LyricRepositoryConfig,
	type RepositoryIndexingOperations,
	type SongRepositoryConfig,
} from '@overture-stack/maestro-common';
import { getRepoInformation, repository } from '@overture-stack/maestro-repository';

/**
 * Creates an object containing indexing operations to be used in the API
 * @param config The configuration object for the `MaestroProvider`, which includes repository information
 * @param indexer An implementation of `ElasticsearchService` used for performing Elasticsearch operations
 * @returns
 */
export const api = (
	repositories: (LyricRepositoryConfig | SongRepositoryConfig)[],
	indexer: ElasticsearchService,
): RepositoryIndexingOperations => {
	/**
	 * Performs asynchronous fetch and indexing operations for a specified repository,
	 * It returns an immediate response and if the repository code is valid then starts the
	 * indexing operation in the next event loop cycle without waiting for the response.
	 *
	 * @param repoCode
	 * @returns
	 */
	const indexRepository = async (repoCode: string): Promise<ApiResult> => {
		const repoInfo = getRepoInformation(repositories, repoCode);

		if (!repoInfo) {
			const message = `Invalid repository code '${repoCode}'`;
			logger.error(`Invalid repository information for repository code '${repoCode}'`);
			return { successful: false, message };
		}

		// Fire the async operation using setImmediate to ensure it runs in the next event loop cycle
		setImmediate(async () => {
			try {
				for await (const items of repository(repoInfo).getRepositoryRecords()) {
					indexer.bulkUpsert(repoInfo.indexName, items);
				}
			} catch (error) {
				const message = error instanceof Error ? error.message : String(error);
				logger.error(`Error found indexing repository records. ${message}`);
			}
		});

		return {
			indexName: repoInfo.indexName,
			successful: true,
		};
	};

	/**
	 * Performs asynchronous fetch and indexing operations for a specified organization
	 * @param repoCode
	 * @param organization
	 * @returns
	 */
	const indexOrganization = async (repoCode: string, organization: string): Promise<ApiResult> => {
		const repoInfo = getRepoInformation(repositories, repoCode);

		if (!repoInfo) {
			const message = `Invalid repository code '${repoCode}'`;
			logger.error(`Invalid repository information for repository code '${repoCode}'`);
			return { successful: false, message };
		}

		// Fire the async operation using setImmediate to ensure it runs in the next event loop cycle
		setImmediate(async () => {
			try {
				for await (const items of repository(repoInfo).getOrganizationRecords({ organization })) {
					indexer.bulkUpsert(repoInfo.indexName, items);
				}
			} catch (error) {
				const message = error instanceof Error ? error.message : String(error);
				logger.error(`Error found indexing repository records. ${message}`);
			}
		});

		return {
			indexName: repoInfo.indexName,
			successful: true,
		};
	};

	/**
	 * Performs asynchronous fetch and indexing operation for a specified record
	 * @param repoCode
	 * @param organization
	 * @param recordId
	 * @returns
	 */
	const indexRecord = async (repoCode: string, organization: string, recordId: string): Promise<ApiResult> => {
		const repoInfo = getRepoInformation(repositories, repoCode);

		if (!repoInfo) {
			const message = `Invalid repository code '${repoCode}'`;
			logger.error(`Invalid repository information for repository code '${repoCode}'`);
			return { successful: false, message };
		}

		// Fetch the record within a repository
		const repoRecord = await repository(repoInfo).getRecord({ organization, id: recordId });

		if (isEmpty(repoRecord)) {
			const message = `Record '${recordId}' not found in organization '${organization}'`;
			logger.error(`Record '${recordId}' not found in organization '${organization}'`);
			return { successful: false, message };
		}

		setImmediate(async () => {
			try {
				// Index records
				indexer.addData(repoInfo.indexName, repoRecord);
			} catch (error) {
				const message = error instanceof Error ? error.message : String(error);
				logger.error(`Error indexing records. ${message}`);
			}
		});

		return {
			indexName: repoInfo.indexName,
			successful: true,
		};
	};

	/**
	 * Performs asynchronous fetch and remove indexing for a specified record
	 * @param repoCode
	 * @param organization
	 * @param recordId
	 * @returns
	 */
	const removeIndexRecord = async (repoCode: string, organization: string, recordId: string): Promise<ApiResult> => {
		const repoInfo = getRepoInformation(repositories, repoCode);

		if (!repoInfo) {
			const message = `Invalid repository code '${repoCode}'`;
			logger.error(`Invalid repository information for repository code '${repoCode}'`);
			return { successful: false, message };
		}

		// Fetch the record within a repository
		const repoRecord = await repository(repoInfo).getRecord({ organization, id: recordId });

		if (isEmpty(repoRecord)) {
			const message = `Record '${recordId}' not found in organization '${organization}'`;
			logger.error(`Record '${recordId}' not found in organization '${organization}'`);
			return { successful: false, message };
		}

		setImmediate(async () => {
			try {
				indexer.deleteData(repoInfo.indexName, recordId);
			} catch (error) {
				const message = error instanceof Error ? error.message : String(error);
				logger.error(`Error found indexing records. ${message}`);
			}
		});

		return {
			indexName: repoInfo.indexName,
			successful: true,
		};
	};

	return {
		indexOrganization,
		indexRecord,
		indexRepository,
		removeIndexRecord,
	};
};
