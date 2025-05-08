export type DataRecordValue = string | string[] | number | number[] | boolean | boolean[] | undefined;

export type FailureData = Record<string, string[]>;

export type IndexResult = {
	indexName: string;
	successful: boolean;
	failureData: FailureData;
};
export interface DataRecordNested {
	[key: string]: DataRecordValue | DataRecordNested | DataRecordNested[];
}
