import type { DataRecordNested, IndexResult } from './dataRecord.js';

/**
 * Interface defining the contract for Elasticsearch service operations.
 */
export interface ElasticsearchService {
	/**
	 * Creates an index in Elasticsearch.
	 *
	 * @param index - The name of the index to create.
	 * @returns A promise that resolves to `true` if the index was successfully created, `false` otherwise.
	 */
	createIndex(index: string): Promise<boolean>;

	/**
	 * Indexes data into a specified Elasticsearch index.
	 *
	 * @param index - The name of the index where the data will be stored.
	 * @param data - The data to be indexed.
	 * @returns A promise that resolves to the result of the indexing operation.
	 */
	addData(index: string, data: DataRecordNested): Promise<IndexResult>;

	/**
	 * Checks the availability of the Elasticsearch service.
	 *
	 * @returns A promise that resolves to `true` if Elasticsearch is reachable, `false` otherwise.
	 */
	ping(): Promise<boolean>;

	/**
	 * Updates data in a specified Elasticsearch index.
	 *
	 * @param index - The name of the index where the document exists.
	 * @param id - The ID of the document to update.
	 * @param data - The new data to update in the document.
	 * @returns A promise that resolves to the result of the update operation.
	 */
	updateData(index: string, id: string, data: DataRecordNested): Promise<IndexResult>;

	/**
	 * Deletes a document from a specified Elasticsearch index.
	 *
	 * @param index - The name of the index from which the document will be deleted.
	 * @param id - The ID of the document to delete.
	 * @returns A promise that resolves to the result of the deletion operation.
	 */
	deleteData(index: string, id: string): Promise<IndexResult>;

	/**
	 * Performs a bulk upsert operation to index or update multiple documents in the specified index.
	 * @param index The name of the index where the documents will be upserted
	 * @param data An array of data records to be upserted.
	 */
	bulkUpsert(index: string, data: DataRecordNested[]): Promise<IndexResult>;
}
