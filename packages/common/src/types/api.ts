/**
 * The answer to an indexing request. On a failure, `serverFault` is `true` when the cause lies with Maestro or a service
 * it depends on, such as Elasticsearch, rather than with the request.
 */
export type ApiResult = {
	indexName?: string;
	message?: string;
	serverFault?: boolean;
	successful: boolean;
};
