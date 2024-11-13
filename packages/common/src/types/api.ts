export type FailureData = {
	failingIds: string[];
};

export type IndexResult = {
	indexName: string;
	successful: boolean;
	failureData: FailureData;
};
