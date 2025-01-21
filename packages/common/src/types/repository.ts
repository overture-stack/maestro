import type { ApiResult } from './api';
import type { DataRecordNested } from './dataRecord';

/**
 * Interface for all types of repositories (i.e. song or lyric)
 */
export interface Repository {
	getRepositoryRecords(): AsyncGenerator<DataRecordNested[], void, unknown>;
	getOrganizationRecords({ organization }: { organization: string }): AsyncGenerator<DataRecordNested[], void, unknown>;
	getRecord({ organization, id }: { organization: string; id: string }): Promise<DataRecordNested>;
}

/**
 * Available operations for indexing data from a repository source
 */
export interface RepositoryIndexingOperations {
	/**
	 * Indexes the specified repository.
	 *
	 * @param repoCode - The unique code of the repository to be indexed.
	 * @returns A promise indicating the completion of the indexing operation.
	 */
	indexRepository(repoCode: string): Promise<ApiResult>;

	/**
	 * Indexes the specified organization within the given repository.
	 *
	 * @param repoCode - The unique code of the repository.
	 * @param organization - The name of the organization to be indexed.
	 * @returns A promise indicating the completion of the indexing operation.
	 */
	indexOrganization(repoCode: string, organization: string): Promise<ApiResult>;

	/**
	 * Indexes a specific record identified by its ID within an organization in the given repository.
	 *
	 * @param repoCode - The unique code of the repository.
	 * @param organization - The name of the organization containing the record.
	 * @param recordId - The unique identifier of the record to be indexed.
	 * @returns A promise indicating the completion of the indexing operation.
	 */
	indexRecord(repoCode: string, organization: string, recordId: string): Promise<ApiResult>;

	/**
	 * Remove index of a specific record identified by its ID within an organization in the given repository.
	 *
	 * @param repoCode - The unique code of the repository.
	 * @param organization - The name of the organization containing the record.
	 * @param recordId - The unique identifier of the record to be removed from the index.
	 * @returns A promise indicating the completion of the indexing operation.
	 */
	removeIndexRecord(repoCode: string, organization: string, recordId: string): Promise<ApiResult>;
}
