import type {
	DataRecordNested,
	DocumentFieldValue,
	ElasticsearchService,
	IndexResult,
	ScanDocumentsOptions,
} from '@overture-stack/maestro-common';

/**
 * An `ElasticsearchService` holding `documents` in memory, recording the documents deleted, the pages upserted and the
 * scans requested, for tests of code that indexes through the service. With `deleteFailureReason`, every deletion fails
 * with that reason, such as `DOCUMENT_NOT_FOUND`, and is still recorded.
 */
export const createRecordingIndexer = (
	documents: DocumentFieldValue[],
	{ deleteFailureReason }: { deleteFailureReason?: string } = {},
): {
	deletedIds: string[];
	indexer: ElasticsearchService;
	scans: { index: string; options: ScanDocumentsOptions }[];
	upsertedPages: DataRecordNested[][];
} => {
	const deletedIds: string[] = [];
	const scans: { index: string; options: ScanDocumentsOptions }[] = [];
	const upsertedPages: DataRecordNested[][] = [];
	const succeeded = (indexName: string): IndexResult => ({ failureData: {}, indexName, successful: true });

	const indexer: ElasticsearchService = {
		addData: async (index) => succeeded(index),
		bulkUpsert: async (index, data) => {
			upsertedPages.push(data);
			return succeeded(index);
		},
		createIndex: async () => true,
		deleteData: async (index, id) => {
			deletedIds.push(id);
			return deleteFailureReason
				? { failureData: { [id]: [deleteFailureReason] }, indexName: index, successful: false }
				: succeeded(index);
		},
		ping: async () => true,
		scanDocuments: async function* (index, options) {
			scans.push({ index, options });
			yield documents.filter(
				(document) => options.fieldValue === undefined || document.fieldValue === options.fieldValue,
			);
		},
		updateData: async (index) => succeeded(index),
	};

	return { deletedIds, indexer, scans, upsertedPages };
};
