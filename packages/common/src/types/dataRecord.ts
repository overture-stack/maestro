export type DataRecordValue = string | string[] | number | number[] | boolean | boolean[] | undefined;

export type FailureData = Record<string, string[]>;

export type IndexResult = {
	indexName: string;
	successful: boolean;
	failureData: FailureData;
};

export interface IndexData {
	id: string;
	data: Record<string, DataRecordValue>;
	organization: string;
	entityName: string;
}
