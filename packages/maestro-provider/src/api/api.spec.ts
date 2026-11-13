import { expect } from 'chai';
import fetchMock from 'fetch-mock';
import { afterEach, beforeEach, describe, it } from 'mocha';

import { DOCUMENT_NOT_FOUND, type LyricRepositoryConfig, RepositoryType } from '@overture-stack/maestro-common';

import { lyricAuditResponse, lyricAuditRow } from '../../testing/fixtures/lyricAudit.js';
import { createRecordingIndexer } from '../../testing/peripherals/recordingIndexer.js';
import { api } from './api.js';

const lyricConfig: LyricRepositoryConfig = {
	baseUrl: 'http://lyric.test',
	categoryId: 7,
	code: 'lyric-repo',
	indexName: 'lyric-index',
	name: 'Lyric repository',
	type: RepositoryType.LYRIC,
	validDataOnly: true,
};

const recordUrl = (id: string): string => `http://lyric.test/data/category/7/id/${id}?view=compound`;

const auditUrl = (organization: string, id: string): string =>
	`http://lyric.test/audit/category/7/organization/${organization}?systemId=${id}&eventType=DELETE`;

const nextEventLoopCycle = (): Promise<void> => new Promise((resolve) => setImmediate(resolve));

describe('api', () => {
	beforeEach(() => {
		fetchMock.mockGlobal();
	});

	afterEach(() => {
		fetchMock.hardReset();
	});

	describe('indexRecord', () => {
		it('removes the document of a record confirmed deleted by Lyric, and reports success', async () => {
			const { deletedIds, indexer } = createRecordingIndexer([]);
			fetchMock.get(recordUrl('gone'), 404);
			fetchMock.get(
				auditUrl('org-a', 'gone'),
				lyricAuditResponse([lyricAuditRow({ organization: 'org-a', systemId: 'gone' })]),
			);

			const result = await api([lyricConfig], indexer).indexRecord('lyric-repo', 'org-a', 'gone');

			expect(deletedIds).to.eql(['gone']);
			expect(result.successful).to.equal(true);
			expect(result.indexName).to.equal('lyric-index');
		});

		it('reports the record as not found, and leaves the index alone, when Lyric cannot confirm a deletion', async () => {
			const { deletedIds, indexer } = createRecordingIndexer([]);
			fetchMock.get(recordUrl('missing'), 404);
			fetchMock.get(auditUrl('org-a', 'missing'), 404);

			const result = await api([lyricConfig], indexer).indexRecord('lyric-repo', 'org-a', 'missing');
			await nextEventLoopCycle();

			expect(deletedIds).to.eql([]);
			expect(result).to.eql({ message: "Record 'missing' not found in organization 'org-a'", successful: false });
		});

		it('leaves the index alone when Lyric fails to answer for the record', async () => {
			const { deletedIds, indexer } = createRecordingIndexer([]);
			fetchMock.get(recordUrl('rec-1'), 503);

			const result = await api([lyricConfig], indexer).indexRecord('lyric-repo', 'org-a', 'rec-1');
			await nextEventLoopCycle();

			expect(deletedIds).to.eql([]);
			expect(result.successful).to.equal(false);
		});
	});

	describe('a record confirmed deleted by Lyric', () => {
		const confirmDeletion = (): void => {
			fetchMock.get(recordUrl('gone'), 404);
			fetchMock.get(
				auditUrl('org-a', 'gone'),
				lyricAuditResponse([lyricAuditRow({ organization: 'org-a', systemId: 'gone' })]),
			);
		};

		it('is reported as handled when the index no longer holds its document', async () => {
			const { indexer } = createRecordingIndexer([], { deleteFailureReason: DOCUMENT_NOT_FOUND });
			confirmDeletion();

			const result = await api([lyricConfig], indexer).indexRecord('lyric-repo', 'org-a', 'gone');

			expect(result.successful).to.equal(true);
			expect(result.serverFault).to.equal(undefined);
		});

		it('is reported as a server fault when the index fails to remove its document', async () => {
			const { indexer } = createRecordingIndexer([], { deleteFailureReason: 'ResponseError' });
			confirmDeletion();

			const result = await api([lyricConfig], indexer).removeIndexRecord('lyric-repo', 'org-a', 'gone');

			expect(result.successful).to.equal(false);
			expect(result.serverFault).to.equal(true);
		});
	});

	describe('removeIndexRecord', () => {
		it('removes the document of a record confirmed deleted by Lyric', async () => {
			const { deletedIds, indexer } = createRecordingIndexer([]);
			fetchMock.get(recordUrl('gone'), 404);
			fetchMock.get(
				auditUrl('org-a', 'gone'),
				lyricAuditResponse([lyricAuditRow({ organization: 'org-a', systemId: 'gone' })]),
			);

			const result = await api([lyricConfig], indexer).removeIndexRecord('lyric-repo', 'org-a', 'gone');

			expect(deletedIds).to.eql(['gone']);
			expect(result.successful).to.equal(true);
		});

		it('removes the document of a record that still exists in the organization', async () => {
			const { deletedIds, indexer } = createRecordingIndexer([]);
			fetchMock.get(recordUrl('rec-1'), { organization: 'org-a', systemId: 'rec-1' });

			const result = await api([lyricConfig], indexer).removeIndexRecord('lyric-repo', 'org-a', 'rec-1');
			await nextEventLoopCycle();

			expect(deletedIds).to.eql(['rec-1']);
			expect(result.successful).to.equal(true);
		});

		it('reports the record as not found, and leaves the index alone, when Lyric cannot confirm a deletion', async () => {
			const { deletedIds, indexer } = createRecordingIndexer([]);
			fetchMock.get(recordUrl('missing'), 404);
			fetchMock.get(auditUrl('org-a', 'missing'), 404);

			const result = await api([lyricConfig], indexer).removeIndexRecord('lyric-repo', 'org-a', 'missing');
			await nextEventLoopCycle();

			expect(deletedIds).to.eql([]);
			expect(result.successful).to.equal(false);
		});
	});
});
