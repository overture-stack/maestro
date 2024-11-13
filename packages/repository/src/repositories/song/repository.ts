import * as path from 'path';

import type { DataRecordValue, IRepository, SongRepositoryConfig } from '@overture-stack/maestro-common';

import httpClient from '../../network/httpClient';
import { isArrayOfObjects } from '../../utils/utils';

/**
 * Implementation to use Song as a data repository
 * @param config
 * @returns
 */
export const songRepository = (config: SongRepositoryConfig): IRepository => {
	const { baseUrl, paginationSize } = config;

	const getOrganizationRecords = async function* ({
		organization,
	}: {
		organization: string;
	}): AsyncGenerator<Record<string, DataRecordValue>[], void, unknown> {
		let offset = 0;
		let hasMoreData = true;

		// Get all analysis paginated for a study
		// http://song/studies/{organization}/analysis/paginated?analysisStates={2}&limit={3,number,#}&offset={4,number,#}
		const fullUrl = new URL(path.join('studies', organization, 'analysis', 'paginated'), baseUrl);
		if (paginationSize) {
			fullUrl.searchParams.append('limit', paginationSize.toString());
		}

		while (hasMoreData) {
			if (paginationSize) {
				fullUrl.searchParams.set('offset', offset.toString());
			}

			const result = await httpClient(fullUrl.toString());

			if (isArrayOfObjects(result?.records)) {
				const validArray: Array<Record<string, DataRecordValue>> = result.records.map(
					(item: Record<string, DataRecordValue>) => ({ ...item }),
				);
				yield validArray;
			} else {
				return;
			}
			if (paginationSize) {
				offset++;
				hasMoreData = offset < result?.pagination?.totalPages;
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
		// Get the analysis by ID
		// http://song/studies/{organization}/analysis/{id}
		const fullUrl = new URL(path.join('studies', organization, 'analysis', id), baseUrl);

		return httpClient(fullUrl.toString());
	};

	const getRepositoryRecords = async function* (): AsyncGenerator<Record<string, DataRecordValue>[], void, unknown> {
		// Get all the studies
		// http://song/studies/all
		const fullUrl = new URL(baseUrl);
		const allStudiesResult = await httpClient(fullUrl.toString());

		for (let index = 1; index <= allStudiesResult.length; index++) {
			const study = allStudiesResult[index];
			// Get Records per study
			for await (const records of getOrganizationRecords({ organization: study })) {
				yield records;
			}
		}
	};

	return {
		getRepositoryRecords,
		getOrganizationRecords,
		getRecord,
	};
};
