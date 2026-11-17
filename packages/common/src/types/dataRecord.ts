export type DataRecordValue = string | string[] | number | number[] | boolean | boolean[] | undefined;

/**
 * The reason given by `ElasticsearchService.deleteData` in `failureData` for a document missing from the index.
 */
export const DOCUMENT_NOT_FOUND = 'not_found';

export type FailureData = Record<string, string[]>;

export type IndexResult = {
	indexName: string;
	successful: boolean;
	failureData: FailureData;
};
export interface DataRecordNested {
	[key: string]: DataRecordValue | DataRecordNested | DataRecordNested[];
}
