import * as path from 'path';

import {
	type DataRecordNested,
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
	ALL: 'all',
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
	}): AsyncGenerator<DataRecordNested[], void, unknown> {
		let offset = 0;
		let hasMoreData = true;

		// Get all analysis paginated for a study
		// http://song/studies/{organization}/analysis/paginated?analysisStates={2}&limit={3,number,#}&offset={4,number,#}
		const fullUrl = new URL(
			path.join(PATH.STUDIES, organization, PATH.ANALYSIS, paginationSize ? PATH.PAGINATED : ''),
			baseUrl,
		);
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
					const parsedRecords = paginationSize ? parsedResponse.analyses : parsedResponse;
					if (isArrayOfObjects(parsedRecords)) {
						yield parsedRecords.map((record) => {
							return { _id: record.analysisId, ...record };
						});
					} else {
						return;
					}
					if (paginationSize) {
						hasMoreData = parseInt(parsedResponse?.currentTotalAnalyses) < parseInt(parsedResponse?.totalAnalyses);
						offset++;
					} else {
						hasMoreData = false;
					}
				} else {
					hasMoreData = false;
				}
			} catch (error) {
				logger.error(`Error fetching Song records on study '${organization}'`, error);
				throw error;
			}
		}
	};

	const getRecord = async ({ organization, id }: { organization: string; id: string }): Promise<DataRecordNested> => {
		// Get the analysis by ID
		// http://song/studies/{organization}/analysis/{id}
		const fullUrl = new URL(path.join(PATH.STUDIES, organization, PATH.ANALYSIS, id), baseUrl);

		const response = await sendHttpRequest(fullUrl.toString());
		if (response.ok) {
			const parsedResponse = await response.json();
			return { _id: parsedResponse.analysisId, ...parsedResponse };
		}
		return {};
	};

	const getAllStudies = async () => {
		// Get all the studies
		// http://song/studies/all
		const fullUrl = new URL(path.join(PATH.STUDIES, PATH.ALL), baseUrl);
		try {
			const response = await sendHttpRequest(fullUrl.toString());
			if (response.ok) {
				const parsedResponse = await response.json();
				if (Array.isArray(parsedResponse)) {
					return parsedResponse;
				}
			}
			return [];
		} catch (error) {
			logger.error(`Error fetching Song all studies`, error);
			throw error;
		}
	};

	const getRepositoryRecords = async function* (): AsyncGenerator<DataRecordNested[], void, unknown> {
		// Get all the studies
		const studies = await getAllStudies();

		for (const study of studies) {
			// Get Records per study
			for await (const records of getOrganizationRecords({ organization: study })) {
				yield records;
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
