import * as path from 'path';

import {
	type DataRecordNested,
	logger,
	type LyricRepositoryConfig,
	type Repository,
} from '@overture-stack/maestro-common';

import { sendHttpRequest } from '../../network/httpRequest';
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
			if (paginationSize) {
				fullUrl.searchParams.set(QUERY.PAGE, page.toString());
			}
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
						return;
					}
					if (paginationSize) {
						hasMoreData = page < parsedResponse?.pagination?.totalPages;
						page++;
					} else {
						hasMoreData = false;
					}
				} else {
					hasMoreData = false;
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
			if (paginationSize) {
				fullUrl.searchParams.set(QUERY.PAGE, page.toString());
			}
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
						return;
					}
					if (paginationSize) {
						hasMoreData = page < parsedResponse?.pagination?.totalPages;
						page++;
					} else {
						hasMoreData = false;
					}
				} else {
					hasMoreData = false;
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

	return {
		getRepositoryRecords,
		getOrganizationRecords,
		getRecord,
	};
};
