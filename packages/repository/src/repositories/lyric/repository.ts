import * as path from 'path';

import {
	type DataRecordValue,
	logger,
	type LyricRepositoryConfig,
	type Repository,
} from '@overture-stack/maestro-common';

import { sendHttpRequest } from '../../network/httpRequest';
import { sanitizeKeyName } from '../../utils/formatter';
import { isArrayOfObjects } from '../../utils/utils';

// Path constants
const PATH = {
	DATA: 'data',
	CATEGORY: 'category',
	ORGANIZATION: 'organization',
	ID: 'id',
} as const;

// Query parameter constants
const QUERY = {
	VIEW: 'view',
	PAGESIZE: 'pageSize',
	PAGE: 'page',
} as const;

// View constant
const VIEW = {
	COMPOUND: 'compound',
} as const;

/**
 * Implementation to use Lyric as a data repository
 * @param config
 * @returns
 */
export const lyricRepository = (config: LyricRepositoryConfig): Repository => {
	const { baseUrl, categoryId, paginationSize } = config;
	const getRepositoryRecords = async function* (): AsyncGenerator<Record<string, DataRecordValue>[], void, unknown> {
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
			if (paginationSize) {
				fullUrl.searchParams.set(QUERY.PAGE, page.toString());
			}
			try {
				const response = await sendHttpRequest(fullUrl.toString());

				if (response.ok) {
					const parsedResponse = await response.json();
					if (isArrayOfObjects(parsedResponse?.records)) {
						const validArray: Array<Record<string, DataRecordValue>> = parsedResponse.records.map(
							(item: Record<string, DataRecordValue>) => {
								const formattedData =
									typeof item.data === 'object'
										? Object.entries(item.data).reduce((newObj: Record<string, DataRecordValue>, [key, value]) => {
												const newKey = sanitizeKeyName(key);
												newObj[newKey] = value;
												return newObj;
											}, {})
										: {};

								return { ...item, data: formattedData };
							},
						);
						yield validArray;
					} else {
						return;
					}
					if (paginationSize) {
						page++;
						hasMoreData = page < parsedResponse?.pagination?.totalPages;
					}
				}

				hasMoreData = false;
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
	}): AsyncGenerator<Record<string, DataRecordValue>[], void, unknown> {
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
			if (paginationSize) {
				fullUrl.searchParams.set(QUERY.PAGE, page.toString());
			}
			try {
				const response = await sendHttpRequest(fullUrl.toString());

				if (response.ok) {
					const parsedResponse = await response.json();
					if (isArrayOfObjects(parsedResponse?.records)) {
						const validArray: Array<Record<string, DataRecordValue>> = parsedResponse.records.map(
							(item: Record<string, DataRecordValue>) => {
								const { systemId, ...rest } = item;
								return { ...rest, id: systemId };
							},
						);
						yield validArray;
					} else {
						return;
					}
					if (paginationSize) {
						page++;
						hasMoreData = page < parsedResponse?.pagination?.totalPages;
					}
				}
				hasMoreData = false;
			} catch (error) {
				logger.error(`Error fetching Lyric records on organization '${organization}'`, error);
				throw error;
			}
		}
	};
	const getRecord = async ({
		organization,
		id,
	}: {
		organization: string;
		id: string;
	}): Promise<Record<string, DataRecordValue>> => {
		// Get a record by ID
		// http://lyric/data/category/{categoryId}/id/{id}
		const fullUrl = new URL(path.join(PATH.DATA, PATH.CATEGORY, categoryId.toString(), PATH.ID, id), baseUrl);
		fullUrl.searchParams.append(QUERY.VIEW, VIEW.COMPOUND);

		try {
			const response = await sendHttpRequest(fullUrl.toString());

			if (response.ok) {
				const parsedResponse = await response.json();
				if (parsedResponse?.['organization'] === organization) {
					const { systemId, ...rest } = parsedResponse;
					return { ...rest, id: systemId };
				}
			}
		} catch (error) {
			logger.error(`Error fetching Lyric records with ID '${id}'`, error);
			throw error;
		}
		return {};
	};

	return {
		getRepositoryRecords,
		getOrganizationRecords,
		getRecord,
	};
};
