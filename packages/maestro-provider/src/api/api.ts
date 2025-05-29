import {
	type ApiResult,
	convertAnalyses,
	type DataRecordNested,
	type ElasticsearchService,
	isEmpty,
	logger,
	type LyricRepositoryConfig,
	type RepositoryIndexingOperations,
	RepositoryType,
	type SongRepositoryConfig,
} from '@overture-stack/maestro-common';

import { getRepoInformation, repository } from '../repository/index.js';

/**
 * Processes and indexes data records based on the repository type.
 *
 * If the repository type is `SONG`, the function converts the provided items into either
 * file-centric or analysis-centric documents (based on the repository's indexing mode),
 * and then bulk upserts them into the configured Elasticsearch index.
 *
 * For `Lyric` repository types, it upserts the provided items without conversion as they
 * are already formatted.
 *
 * @param items - An array of data records to be indexed.
 * @param repoInfo - The configuration object for the repository, which includes the index name and type.
 * @param indexer - An instance of `ElasticsearchService` used for performing indexing operations.
 */
const indexRepositoryData = ({
	items,
	repoInfo,
	indexer,
}: {
	items: DataRecordNested[];
	repoInfo: SongRepositoryConfig | LyricRepositoryConfig;
	indexer: ElasticsearchService;
}) => {
	if (repoInfo.type === RepositoryType.SONG) {
		// convert Song documents into fileCentric or analysisCentric document
		const converted = convertAnalyses(repoInfo, items);

		if (converted.length > 0) {
			indexer.bulkUpsert(repoInfo.indexName, converted);
		} else {
			logger.error(`Error converting records into '${repoInfo.indexingMode}Centric document'`);
		}
	} else {
		// Lyric repository type, upsert items directly
		indexer.bulkUpsert(repoInfo.indexName, items);
	}
};

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
					if (items.length > 0) {
						indexRepositoryData({ repoInfo, items, indexer });
					}
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
					if (items.length > 0) {
						indexRepositoryData({ repoInfo, items, indexer });
					}
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
				indexRepositoryData({ repoInfo, items: [repoRecord], indexer });
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
