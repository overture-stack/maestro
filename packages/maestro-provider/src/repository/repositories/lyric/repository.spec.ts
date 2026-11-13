import { expect } from 'chai';
import fetchMock from 'fetch-mock';
import { afterEach, beforeEach, describe, it } from 'mocha';

import { type DataRecordNested, type LyricRepositoryConfig, RepositoryType } from '@overture-stack/maestro-common';

import { lyricRepository } from './repository.js';

const config: LyricRepositoryConfig = {
	baseUrl: 'http://lyric.test',
	categoryId: 7,
	code: 'lyric-repo',
	indexName: 'lyric-index',
	name: 'Lyric repository',
	paginationSize: 2,
	type: RepositoryType.LYRIC,
	validDataOnly: true,
};

const configWithoutPageSize: LyricRepositoryConfig = { ...config, paginationSize: undefined };

const pageQuery = (page: number, { paginationSize }: LyricRepositoryConfig): string =>
	paginationSize ? `view=compound&pageSize=${paginationSize}&page=${page}` : `view=compound&page=${page}`;

const organizationPageUrl = (organization: string, page: number, repositoryConfig = config): string =>
	`http://lyric.test/data/category/7/organization/${organization}?${pageQuery(page, repositoryConfig)}`;

const categoryPageUrl = (page: number, repositoryConfig = config): string =>
	`http://lyric.test/data/category/7?${pageQuery(page, repositoryConfig)}`;

const lyricPage = (
	records: Array<{ systemId: string }>,
	{ currentPage, totalPages }: { currentPage: number; totalPages: number },
): { pagination: Record<string, number>; records: Array<{ systemId: string }> } => ({
	pagination: { currentPage, pageSize: 20, totalPages, totalRecords: 21 },
	records,
});

const readPages = async (
	pages: AsyncGenerator<DataRecordNested[], void, unknown>,
): Promise<{ error?: unknown; read: DataRecordNested[][] }> => {
	const read: DataRecordNested[][] = [];
	try {
		for await (const page of pages) {
			read.push(page);
		}
		return { read };
	} catch (error) {
		return { error, read };
	}
};

describe('lyricRepository', () => {
	beforeEach(() => {
		fetchMock.mockGlobal();
	});

	afterEach(() => {
		fetchMock.hardReset();
	});

	describe('getOrganizationRecords', () => {
		it('yields nothing when Lyric has no records for the organization', async () => {
			fetchMock.get(organizationPageUrl('org-a', 1), 404);

			const { error, read } = await readPages(
				lyricRepository(config).getOrganizationRecords({ organization: 'org-a' }),
			);

			expect(error).to.equal(undefined);
			expect(read).to.eql([]);
		});

		it('yields the pages read, then stops with an error, when a later page fails', async () => {
			fetchMock.get(organizationPageUrl('org-a', 1), {
				pagination: { currentPage: 1, pageSize: 2, totalPages: 2, totalRecords: 3 },
				records: [{ systemId: 'rec-1' }, { systemId: 'rec-2' }],
			});
			fetchMock.get(organizationPageUrl('org-a', 2), 500);

			const { error, read } = await readPages(
				lyricRepository(config).getOrganizationRecords({ organization: 'org-a' }),
			);

			expect(read).to.eql([
				[
					{ _id: 'rec-1', systemId: 'rec-1' },
					{ _id: 'rec-2', systemId: 'rec-2' },
				],
			]);
			expect(error).to.be.instanceOf(Error);
		});

		it('stops with an error when the first page fails', async () => {
			fetchMock.get(organizationPageUrl('org-a', 1), 503);

			const { error, read } = await readPages(
				lyricRepository(config).getOrganizationRecords({ organization: 'org-a' }),
			);

			expect(read).to.eql([]);
			expect(error).to.be.instanceOf(Error);
		});

		it('stops with an error when a page holds no list of records', async () => {
			fetchMock.get(organizationPageUrl('org-a', 1), { message: 'unexpected' });

			const { error } = await readPages(lyricRepository(config).getOrganizationRecords({ organization: 'org-a' }));

			expect(error).to.be.instanceOf(Error);
		});

		it('reads every page reported by Lyric when no page size is configured', async () => {
			fetchMock.get(
				organizationPageUrl('org-a', 1, configWithoutPageSize),
				lyricPage([{ systemId: 'rec-1' }], { currentPage: 1, totalPages: 2 }),
			);
			fetchMock.get(
				organizationPageUrl('org-a', 2, configWithoutPageSize),
				lyricPage([{ systemId: 'rec-2' }], { currentPage: 2, totalPages: 2 }),
			);

			const { error, read } = await readPages(
				lyricRepository(configWithoutPageSize).getOrganizationRecords({ organization: 'org-a' }),
			);

			expect(error).to.equal(undefined);
			expect(read).to.eql([[{ _id: 'rec-1', systemId: 'rec-1' }], [{ _id: 'rec-2', systemId: 'rec-2' }]]);
		});
	});

	describe('getRepositoryRecords', () => {
		it('yields nothing when Lyric has no records for the category', async () => {
			fetchMock.get(categoryPageUrl(1), 404);

			const { error, read } = await readPages(lyricRepository(config).getRepositoryRecords());

			expect(error).to.equal(undefined);
			expect(read).to.eql([]);
		});

		it('yields the pages read, then stops with an error, when a later page fails', async () => {
			fetchMock.get(categoryPageUrl(1), {
				pagination: { currentPage: 1, pageSize: 2, totalPages: 2, totalRecords: 3 },
				records: [{ systemId: 'rec-1' }],
			});
			fetchMock.get(categoryPageUrl(2), 502);

			const { error, read } = await readPages(lyricRepository(config).getRepositoryRecords());

			expect(read).to.eql([[{ _id: 'rec-1', systemId: 'rec-1' }]]);
			expect(error).to.be.instanceOf(Error);
		});

		it('reads every page reported by Lyric when no page size is configured', async () => {
			fetchMock.get(
				categoryPageUrl(1, configWithoutPageSize),
				lyricPage([{ systemId: 'rec-1' }], { currentPage: 1, totalPages: 2 }),
			);
			fetchMock.get(
				categoryPageUrl(2, configWithoutPageSize),
				lyricPage([{ systemId: 'rec-2' }], { currentPage: 2, totalPages: 2 }),
			);

			const { error, read } = await readPages(lyricRepository(configWithoutPageSize).getRepositoryRecords());

			expect(error).to.equal(undefined);
			expect(read).to.eql([[{ _id: 'rec-1', systemId: 'rec-1' }], [{ _id: 'rec-2', systemId: 'rec-2' }]]);
		});
	});
});
