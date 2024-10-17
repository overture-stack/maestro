export type ElasticSearchBasicAuthConfig = {
	enabled: boolean;
	password?: string;
	user?: string;
};

type ElasticSearchRetryConfig = {
	retryMaxAttempts?: number;
	retryWaitDurationMillis?: number;
};

export type ElasticSearchConfig = {
	basicAuth: ElasticSearchBasicAuthConfig;
	connectionTimeOut?: number;
	docsPerBulkReqMax?: number;
	nodes: string;
	retry?: ElasticSearchRetryConfig;
	version: number;
};
