import * as path from 'path';

import {
	type DataRecordValue,
	logger,
	type LyricRepositoryConfig,
	type Repository,
} from '@overture-stack/maestro-common';

import httpClient from '../../network/httpClient';
import { sanitizeKeyName } from '../../utils/formatter';
import { isArrayOfObjects } from '../../utils/utils';

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
		const fullUrl = new URL(path.join('data', 'category', categoryId.toString()), baseUrl);
		fullUrl.searchParams.append('view', 'compound');
		if (paginationSize) {
			fullUrl.searchParams.append('pageSize', paginationSize.toString());
		}
		while (hasMoreData) {
			if (paginationSize) {
				fullUrl.searchParams.set('page', page.toString());
			}
			try {
				const result = await httpClient(fullUrl.toString());

				if (isArrayOfObjects(result?.records)) {
					const validArray: Array<Record<string, DataRecordValue>> = result.records.map(
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
					hasMoreData = page < result?.pagination?.totalPages;
				}
				hasMoreData = false;
			} catch (error) {
				logger.error(`Error fetching Lyric records on category '${categoryId}'`, error);
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
			path.join('data', 'category', categoryId.toString(), 'organization', organization),
			baseUrl,
		);
		fullUrl.searchParams.append('view', 'compound');
		if (paginationSize) {
			fullUrl.searchParams.append('pageSize', paginationSize.toString());
		}
		while (hasMoreData) {
			if (paginationSize) {
				fullUrl.searchParams.set('page', page.toString());
			}
			try {
				const result = await httpClient(fullUrl.toString());

				if (isArrayOfObjects(result?.records)) {
					const validArray: Array<Record<string, DataRecordValue>> = result.records.map(
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
					hasMoreData = page < result?.pagination?.totalPages;
				} else {
					hasMoreData = false;
				}
			} catch (error) {
				logger.error(`Error fetching Lyric records on organization '${organization}'`, error);
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
		const fullUrl = new URL(path.join('data', 'category', categoryId.toString(), 'id', id), baseUrl);
		fullUrl.searchParams.append('view', 'compound');

		try {
			const result = await httpClient(fullUrl.toString());
			if (result?.['organization'] === organization) {
				const { systemId, ...rest } = result;
				return { ...rest, id: systemId };
			}
		} catch (error) {
			logger.error(`Error fetching Lyric records with ID '${id}'`, error);
		}
		return {};
	};

	return {
		getRepositoryRecords,
		getOrganizationRecords,
		getRecord,
	};
};
