import * as path from 'path';

import type { DataRecordValue, IRepository, LyricRepositoryConfig } from '@overture-stack/maestro-common';

import httpClient from '../../network/httpClient';
import { isArrayOfObjects } from '../../utils/utils';

/**
 * Implementation to use Lyric as a data repository
 * @param config
 * @returns
 */
export const lyricRepository = (config: LyricRepositoryConfig): IRepository => {
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
			}
			hasMoreData = false;
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

		const result = await httpClient(fullUrl.toString());
		if (result?.['organization'] === organization) {
			const { systemId, ...rest } = result;
			return { ...rest, id: systemId };
		}
		return {};
	};

	return {
		getRepositoryRecords,
		getOrganizationRecords,
		getRecord,
	};
};
