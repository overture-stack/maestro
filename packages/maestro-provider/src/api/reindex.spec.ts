import { expect } from 'chai';
import fetchMock from 'fetch-mock';
import { afterEach, beforeEach, describe, it } from 'mocha';

import {
	type DataRecordNested,
	IndexableState,
	IndexingMode,
	type LyricRepositoryConfig,
	RepositoryType,
	type SongRepositoryConfig,
} from '@overture-stack/maestro-common';

import { lyricAuditResponse, lyricAuditRow } from '../../testing/fixtures/lyricAudit.js';
import { createRecordingIndexer } from '../../testing/peripherals/recordingIndexer.js';
import { reindex } from './reindex.js';

const lyricConfig: LyricRepositoryConfig = {
	baseUrl: 'http://lyric.test',
	categoryId: 7,
	code: 'lyric-repo',
	indexName: 'lyric-index',
	name: 'Lyric repository',
	type: RepositoryType.LYRIC,
	validDataOnly: true,
};

const songConfig: SongRepositoryConfig = {
	baseUrl: 'http://song.test',
	code: 'song-repo',
	indexableStudyStates: [IndexableState.PUBLISHED],
	indexingMode: IndexingMode.fileCentric,
	indexName: 'song-index',
	name: 'Song repository',
	type: RepositoryType.SONG,
};

const recordUrl = (id: string): string => `http://lyric.test/data/category/7/id/${id}?view=compound`;

const auditUrl = (organization: string, id: string): string =>
	`http://lyric.test/audit/category/7/organization/${organization}?systemId=${id}&eventType=DELETE`;

const confirmDeleted = (organization: string, id: string): void => {
	fetchMock.get(recordUrl(id), 404);
	fetchMock.get(auditUrl(organization, id), lyricAuditResponse([lyricAuditRow({ organization, systemId: id })]));
};

const pagesOf = async function* (...pages: DataRecordNested[][]): AsyncGenerator<DataRecordNested[], void, unknown> {
	yield* pages;
};

const record = (id: string, organization: string): DataRecordNested => ({ _id: id, organization, systemId: id });

describe('reindex', () => {
	beforeEach(() => {
		fetchMock.mockGlobal();
	});

	afterEach(() => {
		fetchMock.hardReset();
	});

	it('upserts every page the pass yields', async () => {
		const { indexer, upsertedPages } = createRecordingIndexer([]);
		const pages = pagesOf([record('rec-1', 'org-a'), record('rec-2', 'org-a')], [record('rec-3', 'org-a')]);

		await reindex({ indexer, organization: 'org-a', pages, repoInfo: lyricConfig });

		expect(upsertedPages.map((page) => page.map((item) => item._id))).to.eql([['rec-1', 'rec-2'], ['rec-3']]);
	});

	it('removes a document missing from the pass when Lyric confirms its record deleted', async () => {
		const { deletedIds, indexer } = createRecordingIndexer([
			{ documentId: 'rec-1', fieldValue: 'org-a' },
			{ documentId: 'gone', fieldValue: 'org-a' },
		]);
		confirmDeleted('org-a', 'gone');

		await reindex({
			indexer,
			organization: 'org-a',
			pages: pagesOf([record('rec-1', 'org-a')]),
			repoInfo: lyricConfig,
		});

		expect(deletedIds).to.eql(['gone']);
	});

	it('keeps a document missing from the pass while its record still exists', async () => {
		const { deletedIds, indexer } = createRecordingIndexer([{ documentId: 'late', fieldValue: 'org-a' }]);
		fetchMock.get(recordUrl('late'), { organization: 'org-a', systemId: 'late' });

		await reindex({ indexer, organization: 'org-a', pages: pagesOf(), repoInfo: lyricConfig });

		expect(deletedIds).to.eql([]);
	});

	it('keeps a document when Lyric cannot confirm its record deleted', async () => {
		const { deletedIds, indexer } = createRecordingIndexer([{ documentId: 'unconfirmed', fieldValue: 'org-a' }]);
		fetchMock.get(recordUrl('unconfirmed'), 404);
		fetchMock.get(auditUrl('org-a', 'unconfirmed'), 404);

		await reindex({ indexer, organization: 'org-a', pages: pagesOf(), repoInfo: lyricConfig });

		expect(deletedIds).to.eql([]);
	});

	it('removes nothing, and asks Lyric nothing, when the pass stops with an error', async () => {
		const { deletedIds, indexer, scans } = createRecordingIndexer([{ documentId: 'gone', fieldValue: 'org-a' }]);
		confirmDeleted('org-a', 'gone');
		const failingPages = async function* (): AsyncGenerator<DataRecordNested[], void, unknown> {
			yield [record('rec-1', 'org-a')];
			throw new Error('Lyric answered 500 for page 2');
		};

		await reindex({ indexer, organization: 'org-a', pages: failingPages(), repoInfo: lyricConfig });

		expect(deletedIds).to.eql([]);
		expect(scans).to.eql([]);
		expect(fetchMock.callHistory.calls()).to.eql([]);
	});

	it("lists only the organization's documents when reindexing an organization", async () => {
		const { indexer, scans } = createRecordingIndexer([]);

		await reindex({ indexer, organization: 'org-a', pages: pagesOf(), repoInfo: lyricConfig });

		expect(scans).to.eql([{ index: 'lyric-index', options: { fieldName: 'organization', fieldValue: 'org-a' } }]);
	});

	it('checks each document against its own organization when reindexing the whole repository', async () => {
		const { deletedIds, indexer, scans } = createRecordingIndexer([
			{ documentId: 'gone-a', fieldValue: 'org-a' },
			{ documentId: 'gone-b', fieldValue: 'org-b' },
		]);
		confirmDeleted('org-a', 'gone-a');
		confirmDeleted('org-b', 'gone-b');

		await reindex({ indexer, pages: pagesOf(), repoInfo: lyricConfig });

		expect(scans).to.eql([{ index: 'lyric-index', options: { fieldName: 'organization' } }]);
		expect(deletedIds).to.eql(['gone-a', 'gone-b']);
	});

	it('keeps a document holding no organization when reindexing the whole repository', async () => {
		const { deletedIds, indexer } = createRecordingIndexer([{ documentId: 'orphan', fieldValue: undefined }]);

		await reindex({ indexer, pages: pagesOf(), repoInfo: lyricConfig });

		expect(deletedIds).to.eql([]);
		expect(fetchMock.callHistory.calls()).to.eql([]);
	});

	it('removes nothing, and asks Lyric nothing, when the indexer cannot list documents', async () => {
		const { deletedIds, indexer } = createRecordingIndexer([{ documentId: 'gone', fieldValue: 'org-a' }]);
		confirmDeleted('org-a', 'gone');

		await reindex({
			indexer: { ...indexer, scanDocuments: undefined },
			organization: 'org-a',
			pages: pagesOf(),
			repoInfo: lyricConfig,
		});

		expect(deletedIds).to.eql([]);
		expect(fetchMock.callHistory.calls()).to.eql([]);
	});

	it('removes nothing from a Song repository', async () => {
		const { deletedIds, indexer, scans } = createRecordingIndexer([
			{ documentId: 'analysis-1', fieldValue: 'study-a' },
		]);

		await reindex({ indexer, organization: 'study-a', pages: pagesOf(), repoInfo: songConfig });

		expect(scans).to.eql([]);
		expect(deletedIds).to.eql([]);
	});
});
