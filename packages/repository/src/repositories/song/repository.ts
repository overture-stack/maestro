import * as path from 'path';

import {
	type DataRecordValue,
	logger,
	type Repository,
	type SongRepositoryConfig,
} from '@overture-stack/maestro-common';

import { sendHttpRequest } from '../../network/httpRequest';
import { isArrayOfObjects } from '../../utils/utils';

// Path constants
const PATH = {
	STUDIES: 'studies',
	ANALYSIS: 'analysis',
	PAGINATED: 'paginated',
} as const;

// Query parameter constants
const QUERY = {
	LIMIT: 'limit',
	OFFSET: 'offset',
} as const;

/**
 * Implementation to use Song as a data repository
 * @param config
 * @returns
 */
export const songRepository = (config: SongRepositoryConfig): Repository => {
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
		const fullUrl = new URL(path.join(PATH.STUDIES, organization, PATH.ANALYSIS, PATH.PAGINATED), baseUrl);
		if (paginationSize) {
			fullUrl.searchParams.append(QUERY.LIMIT, paginationSize.toString());
		}

		while (hasMoreData) {
			if (paginationSize) {
				fullUrl.searchParams.set(QUERY.OFFSET, offset.toString());
			}

			try {
				const response = await sendHttpRequest(fullUrl.toString());

				if (response.ok) {
					const parsedResponse = await response.json();
					if (isArrayOfObjects(parsedResponse?.records)) {
						const validArray: Array<Record<string, DataRecordValue>> = parsedResponse.records.map(
							(item: Record<string, DataRecordValue>) => ({ ...item }),
						);
						yield validArray;
					} else {
						return;
					}
					if (paginationSize) {
						offset++;
						hasMoreData = offset < parsedResponse?.pagination?.totalPages;
					}
				}

				hasMoreData = false;
			} catch (error) {
				logger.error(`Error fetching Song records on study '${organization}'`, error);
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
		// Get the analysis by ID
		// http://song/studies/{organization}/analysis/{id}
		const fullUrl = new URL(path.join(PATH.STUDIES, organization, PATH.ANALYSIS, id), baseUrl);

		const response = await sendHttpRequest(fullUrl.toString());
		if (response.ok) {
			return await response.json();
		}
		return {};
	};

	const getRepositoryRecords = async function* (): AsyncGenerator<Record<string, DataRecordValue>[], void, unknown> {
		// Get all the studies
		// http://song/studies/all
		const fullUrl = new URL(baseUrl);
		const response = await sendHttpRequest(fullUrl.toString());

		if (response.ok) {
			const allStudiesResult = await response.json();
			for (let index = 1; index <= allStudiesResult.length; index++) {
				const study = allStudiesResult[index];
				// Get Records per study
				for await (const records of getOrganizationRecords({ organization: study })) {
					yield records;
				}
			}
		}
		return;
	};

	return {
		getRepositoryRecords,
		getOrganizationRecords,
		getRecord,
	};
};
