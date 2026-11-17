import type { DocumentFieldValue } from '@overture-stack/maestro-common';

/**
 * The number of documents `scanDocuments` fetches per request when its options give no page size.
 */
export const DEFAULT_SCAN_PAGE_SIZE = 1000;

/**
 * How long Elasticsearch keeps a scan's position between two of its requests.
 */
export const SCROLL_KEEP_ALIVE = '1m';

/**
 * The query selecting the documents a scan lists.
 */
export type ScanQuery = { match_all: Record<string, never> } | { term: Record<string, string> };

const isRecord = (value: unknown): value is Record<string, unknown> => typeof value === 'object' && value !== null;

/**
 * Builds the query for a scan. Every document matches when `fieldValue` is omitted. Otherwise the query matches the
 * documents holding `fieldValue` in `fieldName` as a whole, which requires the index to map `fieldName` as `keyword`.
 */
export const buildScanQuery = (fieldName: string, fieldValue?: string): ScanQuery =>
	fieldValue === undefined ? { match_all: {} } : { term: { [fieldName]: fieldValue } };

/**
 * Pairs each hit's document ID with the value its document holds in `fieldName`. With `fieldValue`, keeps only the
 * documents holding exactly that value.
 */
export const toDocumentFieldValues = (
	hits: Array<{ _id?: unknown; _source?: unknown }>,
	fieldName: string,
	fieldValue?: string,
): DocumentFieldValue[] =>
	hits
		.flatMap((hit) =>
			typeof hit._id === 'string'
				? [{ documentId: hit._id, fieldValue: isRecord(hit._source) ? hit._source[fieldName] : undefined }]
				: [],
		)
		.filter((document) => fieldValue === undefined || document.fieldValue === fieldValue);

/**
 * Whether `error` is Elasticsearch's answer to deleting a document missing from its index. A missing index also
 * answers 404, with an error body instead of a `not_found` result, and does not match.
 */
export const isDocumentNotFound = (error: unknown): boolean =>
	isRecord(error) &&
	isRecord(error.meta) &&
	error.meta.statusCode === 404 &&
	isRecord(error.meta.body) &&
	error.meta.body.result === 'not_found';
