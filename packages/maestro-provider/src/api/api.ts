import {
	type ApiResult,
	type ElasticsearchService,
	isEmpty,
	logger,
	type LyricRepositoryConfig,
	type RepositoryIndexingOperations,
	RepositoryType,
	type SongRepositoryConfig,
} from '@overture-stack/maestro-common';

import { getRepoInformation, repository } from '../repository/index.js';
import { lyricRepository } from '../repository/repositories/lyric/repository.js';
import { indexRepositoryData, reindex, REMOVAL_OUTCOME, removeDocumentIfRecordDeleted } from './reindex.js';

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
	const notFound = ({ organization, recordId }: { organization: string; recordId: string }): ApiResult => {
		const message = `Record '${recordId}' not found in organization '${organization}'`;
		logger.error(message);
		return { message, successful: false };
	};

	/**
	 * Removes the document of a record confirmed deleted by Lyric, and answers `undefined` for any other record.
	 */
	const removeIfRecordDeleted = async ({
		organization,
		recordId,
		repoInfo,
	}: {
		organization: string;
		recordId: string;
		repoInfo: LyricRepositoryConfig | SongRepositoryConfig;
	}): Promise<ApiResult | undefined> => {
		if (repoInfo.type === RepositoryType.LYRIC) {
			const outcome = await removeDocumentIfRecordDeleted({
				documentId: recordId,
				indexer,
				indexName: repoInfo.indexName,
				lyric: lyricRepository(repoInfo),
				organization,
			});
			const deletedRecord = `Record '${recordId}' was deleted from organization '${organization}'`;
			switch (outcome) {
				case REMOVAL_OUTCOME.REMOVED:
					return {
						indexName: repoInfo.indexName,
						message: `${deletedRecord}, so its document is removed from the index`,
						successful: true,
					};
				case REMOVAL_OUTCOME.ALREADY_ABSENT:
					return {
						indexName: repoInfo.indexName,
						message: `${deletedRecord}, and the index holds no document for it`,
						successful: true,
					};
				case REMOVAL_OUTCOME.FAILED:
					return {
						indexName: repoInfo.indexName,
						message: `${deletedRecord}, but the index could not remove its document`,
						serverFault: true,
						successful: false,
					};
				case REMOVAL_OUTCOME.KEPT:
					return undefined;
			}
		}
		return undefined;
	};

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
				await reindex({ indexer, pages: repository(repoInfo).getRepositoryRecords(), repoInfo });
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
				await reindex({
					indexer,
					organization,
					pages: repository(repoInfo).getOrganizationRecords({ organization }),
					repoInfo,
				});
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
			return (
				(await removeIfRecordDeleted({ organization, recordId, repoInfo })) ?? notFound({ organization, recordId })
			);
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
			return (
				(await removeIfRecordDeleted({ organization, recordId, repoInfo })) ?? notFound({ organization, recordId })
			);
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
