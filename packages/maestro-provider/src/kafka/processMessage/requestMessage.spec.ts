import { expect } from 'chai';
import type { KafkaMessage } from 'kafkajs';
import { describe, it } from 'mocha';

import {
	type ApiResult,
	type LyricRepositoryConfig,
	type RepositoryIndexingOperations,
	RepositoryType,
} from '@overture-stack/maestro-common';

import { createRecordingIndexer } from '../../../testing/peripherals/recordingIndexer.js';
import { processRequestMessage } from './requestMessage.js';

const lyricConfig: LyricRepositoryConfig = {
	baseUrl: 'http://lyric.test',
	categoryId: 7,
	code: 'lyric-repo',
	indexName: 'lyric-index',
	name: 'Lyric repository',
	type: RepositoryType.LYRIC,
	validDataOnly: true,
};

const requestMessage = (payload: Record<string, string>): KafkaMessage => ({
	attributes: 0,
	headers: {},
	key: null,
	offset: '0',
	timestamp: '0',
	value: Buffer.from(JSON.stringify(payload)),
});

const accepted: ApiResult = { successful: true };

const indexingOperations = (
	indexRecord: RepositoryIndexingOperations['indexRecord'],
): RepositoryIndexingOperations => ({
	indexOrganization: async () => accepted,
	indexRecord,
	indexRepository: async () => accepted,
	removeIndexRecord: async () => accepted,
});

describe('processRequestMessage', () => {
	it('fails when indexing the requested record fails', async () => {
		const failure = new Error('indexing failed');
		const { indexer } = createRecordingIndexer([]);

		const outcome = await processRequestMessage({
			indexer,
			message: requestMessage({ analysisId: 'rec-1', repositoryCode: 'lyric-repo', studyId: 'org-a' }),
			repositories: [lyricConfig],
			repositoryIndexingApi: indexingOperations(async () => {
				throw failure;
			}),
		}).then(
			() => undefined,
			(error: unknown) => error,
		);

		expect(outcome).to.equal(failure);
	});
});
