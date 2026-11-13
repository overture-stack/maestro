import * as path from 'path';

import {
	type DataRecordNested,
	logger,
	type LyricRepositoryConfig,
	type Repository,
} from '@overture-stack/maestro-common';

import { sendHttpRequest } from '../../network/httpRequest.js';
import { isArrayOfObjects } from '../../utils/utils.js';

// Path constants
const PATH = {
	AUDIT: 'audit',
	CATEGORY: 'category',
	DATA: 'data',
	ID: 'id',
	ORGANIZATION: 'organization',
} as const;

// Query parameter constants
const QUERY = {
	EVENT_TYPE: 'eventType',
	PAGE: 'page',
	PAGESIZE: 'pageSize',
	SYSTEM_ID: 'systemId',
	VIEW: 'view',
} as const;

// View constant
const VIEW = {
	COMPOUND: 'compound',
} as const;

const AUDIT_EVENT_DELETE = 'DELETE';

/**
 * Whether a record exists in Lyric, was deleted from it, or neither can be established.
 */
export const RECORD_STATUS = {
	DELETED: 'deleted',
	PRESENT: 'present',
	UNKNOWN: 'unknown',
} as const;

/**
 * One of the values of `RECORD_STATUS`.
 */
export type RecordStatus = (typeof RECORD_STATUS)[keyof typeof RECORD_STATUS];

/**
 * A `Repository` reading from Lyric. Its `getRecordStatus` answers `present` when Lyric returns the record, whichever
 * organization holds it, and `deleted` when Lyric answers 404 for the record and Lyric's audit lists its deletion in
 * `organization`. Any other answer, or a failed request, gives `unknown`.
 */
export type LyricRepository = Repository & {
	getRecordStatus({ id, organization }: { id: string; organization: string }): Promise<RecordStatus>;
};

/**
 * Implementation to use Lyric as a data repository
 * @param config
 * @returns
 */
export const lyricRepository = (config: LyricRepositoryConfig): LyricRepository => {
	const { baseUrl, categoryId, paginationSize } = config;
	const getRepositoryRecords = async function* (): AsyncGenerator<DataRecordNested[], void, unknown> {
		let page = 1;
		let hasMoreData = true;

		// Get all records paginated by category
		// http://lyric/data/category/{categoryId}?view=compound&page={pageNumber}&pageSize={pageSize}
		const fullUrl = new URL(path.join(PATH.DATA, PATH.CATEGORY, categoryId.toString()), baseUrl);
		fullUrl.searchParams.append(QUERY.VIEW, VIEW.COMPOUND);
		if (paginationSize) {
			fullUrl.searchParams.append(QUERY.PAGESIZE, paginationSize.toString());
		}
		while (hasMoreData) {
			fullUrl.searchParams.set(QUERY.PAGE, page.toString());
			try {
				const response = await sendHttpRequest(fullUrl.toString());

				if (response.ok) {
					const parsedResponse = await response.json();
					if (isArrayOfObjects(parsedResponse?.records)) {
						const validArray: Array<DataRecordNested> = parsedResponse.records.map((item: DataRecordNested) => {
							return { ...item, _id: item.systemId };
						});
						yield validArray;
					} else {
						throw new Error(`Lyric answered page ${page} of category '${categoryId}' without a list of records`);
					}
					hasMoreData = page < parsedResponse?.pagination?.totalPages;
					page++;
				} else if (response.status === 404 && page === 1) {
					// Lyric answers 404 for a category holding no records
					hasMoreData = false;
				} else {
					throw new Error(`Lyric answered ${response.status} for page ${page} of category '${categoryId}'`);
				}
			} catch (error) {
				logger.error(`Error fetching Lyric records on category '${categoryId}'`, error);
				throw error;
			}
		}
	};
	const getOrganizationRecords = async function* ({
		organization,
	}: {
		organization: string;
	}): AsyncGenerator<DataRecordNested[], void, unknown> {
		let page = 1;
		let hasMoreData = true;

		// Get all records paginated by organization
		// http://lyric/data/category/{categoryId}/organization/{organization}?view=compound&page={pageNumber}&pageSize={pageSize}
		const fullUrl = new URL(
			path.join(PATH.DATA, PATH.CATEGORY, categoryId.toString(), PATH.ORGANIZATION, organization),
			baseUrl,
		);
		fullUrl.searchParams.append(QUERY.VIEW, VIEW.COMPOUND);
		if (paginationSize) {
			fullUrl.searchParams.append(QUERY.PAGESIZE, paginationSize.toString());
		}
		while (hasMoreData) {
			fullUrl.searchParams.set(QUERY.PAGE, page.toString());
			try {
				const response = await sendHttpRequest(fullUrl.toString());

				if (response.ok) {
					const parsedResponse = await response.json();
					if (isArrayOfObjects(parsedResponse?.records)) {
						const validArray: Array<DataRecordNested> = parsedResponse.records.map((item: DataRecordNested) => {
							return { ...item, _id: item.systemId };
						});
						yield validArray;
					} else {
						throw new Error(`Lyric answered page ${page} of organization '${organization}' without a list of records`);
					}
					hasMoreData = page < parsedResponse?.pagination?.totalPages;
					page++;
				} else if (response.status === 404 && page === 1) {
					// Lyric answers 404 for an organization holding no records
					hasMoreData = false;
				} else {
					throw new Error(`Lyric answered ${response.status} for page ${page} of organization '${organization}'`);
				}
			} catch (error) {
				logger.error(`Error fetching Lyric records on organization '${organization}'`, error);
				throw error;
			}
		}
	};
	const getRecord = async ({ organization, id }: { organization: string; id: string }): Promise<DataRecordNested> => {
		// Get a record by ID
		// http://lyric/data/category/{categoryId}/id/{id}
		const fullUrl = new URL(path.join(PATH.DATA, PATH.CATEGORY, categoryId.toString(), PATH.ID, id), baseUrl);
		fullUrl.searchParams.append(QUERY.VIEW, VIEW.COMPOUND);

		try {
			const response = await sendHttpRequest(fullUrl.toString());

			if (response.ok) {
				const parsedResponse = await response.json();
				if (parsedResponse?.['organization'] === organization) {
					return { ...parsedResponse, _id: parsedResponse.systemId };
				}
			}
		} catch (error) {
			logger.error(`Error fetching Lyric records with ID '${id}'`, error);
			throw error;
		}
		return {};
	};

	const isDeletionAudited = async ({ id, organization }: { id: string; organization: string }): Promise<boolean> => {
		// http://lyric/audit/category/{categoryId}/organization/{organization}?systemId={id}&eventType=DELETE
		const fullUrl = new URL(
			path.join(PATH.AUDIT, PATH.CATEGORY, categoryId.toString(), PATH.ORGANIZATION, encodeURIComponent(organization)),
			baseUrl,
		);
		fullUrl.searchParams.append(QUERY.SYSTEM_ID, id);
		fullUrl.searchParams.append(QUERY.EVENT_TYPE, AUDIT_EVENT_DELETE);

		const response = await sendHttpRequest(fullUrl.toString());
		if (response.ok) {
			const parsedResponse = await response.json();
			return (
				isArrayOfObjects(parsedResponse?.records) &&
				parsedResponse.records.some(
					(auditRecord: Record<string, unknown>) =>
						auditRecord.event === AUDIT_EVENT_DELETE && auditRecord.systemId === id,
				)
			);
		}
		return false;
	};

	const getRecordStatus = async ({ id, organization }: { id: string; organization: string }): Promise<RecordStatus> => {
		// http://lyric/data/category/{categoryId}/id/{id}
		const fullUrl = new URL(
			path.join(PATH.DATA, PATH.CATEGORY, categoryId.toString(), PATH.ID, encodeURIComponent(id)),
			baseUrl,
		);
		fullUrl.searchParams.append(QUERY.VIEW, VIEW.COMPOUND);

		try {
			const response = await sendHttpRequest(fullUrl.toString());
			if (response.ok) {
				return RECORD_STATUS.PRESENT;
			}
			// A misrouted request also answers 404, so only Lyric's audit of the deletion confirms one
			if (response.status === 404 && (await isDeletionAudited({ id, organization }))) {
				return RECORD_STATUS.DELETED;
			}
			return RECORD_STATUS.UNKNOWN;
		} catch (error) {
			logger.warn(`Could not establish whether Lyric record '${id}' was deleted`, error);
			return RECORD_STATUS.UNKNOWN;
		}
	};

	return {
		getOrganizationRecords,
		getRecord,
		getRecordStatus,
		getRepositoryRecords,
	};
};
