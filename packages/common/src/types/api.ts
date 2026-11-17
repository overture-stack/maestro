/**
 * The answer to an indexing request. `successful` is `false` when the request cannot be carried out as given, which the
 * server answers with `400`. A failure on Maestro's side, or in a service it depends on, is thrown instead. The server
 * answers an error from `utils/errors.ts` with that error's status, and any other error with `500`.
 */
export type ApiResult = {
	indexName?: string;
	message?: string;
	successful: boolean;
};
