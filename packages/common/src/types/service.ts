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
	 * @returns A promise that resolves to the result of the deletion operation. For a document missing from the index,
	 * `successful` is `false` and `failureData[id]` holds `DOCUMENT_NOT_FOUND`.
	 */
	deleteData(index: string, id: string): Promise<IndexResult>;

	/**
	 * Performs a bulk upsert operation to index or update multiple documents in the specified index.
	 * @param index The name of the index where the documents will be upserted
	 * @param data An array of data records to be upserted.
	 */
	bulkUpsert(index: string, data: DataRecordNested[]): Promise<IndexResult>;

	/**
	 * Lists the documents of an index, one page per request, each with the value of one field.
	 *
	 * @param index - The name of the index to list.
	 * @param options - The field to return, and the value a listed document must hold in it.
	 * @returns The pages of documents, in no particular order.
	 */
	scanDocuments?(index: string, options: ScanDocumentsOptions): AsyncGenerator<DocumentFieldValue[], void, unknown>;
}

/**
 * A document's ID, with the value its document holds in the field named by `ScanDocumentsOptions.fieldName`, or
 * `undefined` where the document holds none.
 */
export type DocumentFieldValue = {
	documentId: string;
	fieldValue: unknown;
};

/**
 * Selects the documents `ElasticsearchService.scanDocuments` lists. `fieldName` names the field whose value comes with
 * each document. With `fieldValue`, only documents holding exactly that value in `fieldName` are listed, compared
 * case-sensitively and as a whole, which requires the index to map `fieldName` as `keyword`. `pageSize` is the number
 * of documents fetched per request, 1000 when omitted.
 */
export type ScanDocumentsOptions = {
	fieldName: string;
	fieldValue?: string;
	pageSize?: number;
};
