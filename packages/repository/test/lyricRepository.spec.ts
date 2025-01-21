import { expect } from 'chai';
import fetchMock from 'fetch-mock';
import { describe, it } from 'mocha';

import { type DataRecordNested, type LyricRepositoryConfig, RepositoryType } from '@overture-stack/maestro-common';

import { lyricRepository } from '../src/repositories/lyric/repository';

describe('Lyric Repository', () => {
	beforeEach(() => {
		fetchMock.mockGlobal();
	});

	afterEach(() => {
		fetchMock.hardReset();
	});
	describe('getRepositoryRecords', () => {
		it('should successfully fetch repository records', async () => {
			const config: LyricRepositoryConfig = {
				type: RepositoryType.LYRIC,
				name: 'Lyric Repository',
				categoryId: 123,
				code: 'lyricRepo1',
				baseUrl: 'http://localhost',
				validDataOnly: true,
				indexName: 'index_1_1',
			};

			// Mock response
			fetchMock.any({
				records: [
					{ id: 1, data: { name: 'test' } },
					{ id: 2, data: { name: 'test 2' } },
				],
			});

			const records: DataRecordNested[] = [];

			for await (const items of lyricRepository(config).getRepositoryRecords()) {
				records.push(...items);
			}

			expect(fetchMock.callHistory.calls().length).to.eql(1);
			expect(records.length).to.eql(2);
		});

		it('should successfully fetch repository records with pagination', async () => {
			const config: LyricRepositoryConfig = {
				type: RepositoryType.LYRIC,
				name: 'Lyric Repository',
				categoryId: 123,
				code: 'lyricRepo1',
				baseUrl: 'http://localhost',
				validDataOnly: true,
				indexName: 'index_1_1',
				paginationSize: 2,
			};

			// Mock response
			const urlPage1 = 'http://localhost/data/category/123?view=compound&pageSize=2&page=1';
			fetchMock.get(urlPage1, {
				records: [
					{ id: 1, data: { name: 'test' } },
					{ id: 2, data: { name: 'test 2' } },
				],
				pagination: {
					currentPage: 1,
					pageSize: 2,
					totalPages: 3,
					totalRecords: 5,
				},
			});
			const urlPage2 = 'http://localhost/data/category/123?view=compound&pageSize=2&page=2';
			fetchMock.get(urlPage2, {
				records: [
					{ id: 3, data: { name: 'test 3' } },
					{ id: 4, data: { name: 'test 4' } },
				],
				pagination: {
					currentPage: 2,
					pageSize: 2,
					totalPages: 3,
					totalRecords: 5,
				},
			});
			const urlPage3 = 'http://localhost/data/category/123?view=compound&pageSize=2&page=3';
			fetchMock.get(urlPage3, {
				records: [{ id: 5, data: { name: 'test 5' } }],
				pagination: {
					currentPage: 3,
					pageSize: 2,
					totalPages: 3,
					totalRecords: 5,
				},
			});

			const records: DataRecordNested[] = [];

			for await (const items of lyricRepository(config).getRepositoryRecords()) {
				records.push(...items);
			}

			expect(fetchMock.callHistory.calls().length).to.eql(3);
			expect(records.length).to.eql(5);
		});

		it('should return no records when receive different than 200 OK response', async () => {
			const config: LyricRepositoryConfig = {
				type: RepositoryType.LYRIC,
				name: 'Lyric Repository',
				categoryId: 123,
				code: 'lyricRepo1',
				baseUrl: 'http://localhost',
				validDataOnly: true,
				indexName: 'index_1_1',
				paginationSize: 100,
			};

			// Mock response
			fetchMock.any(401);

			const records: DataRecordNested[] = [];

			for await (const items of lyricRepository(config).getRepositoryRecords()) {
				records.push(...items);
			}

			expect(fetchMock.callHistory.calls().length).to.eql(1);
			expect(records.length).to.eql(0);
		});

		it('should throw an error if an error occcured', async () => {
			const config: LyricRepositoryConfig = {
				type: RepositoryType.LYRIC,
				name: 'Lyric Repository',
				categoryId: 123,
				code: 'lyricRepo1',
				baseUrl: 'http://localhost',
				validDataOnly: true,
				indexName: 'index_1_1',
				paginationSize: 100,
			};

			// Mock response
			fetchMock.any({ throws: { message: 'Network error' } });

			const records: DataRecordNested[] = [];

			try {
				for await (const items of lyricRepository(config).getRepositoryRecords()) {
					records.push(...items);
				}
			} catch (error) {
				expect(fetchMock.callHistory.calls().length).to.eql(1);
				expect(error.message).to.eql('Network error');
				expect(records.length).to.eql(0);
			}
		});
	});

	describe('getOrganizationRecords', () => {
		const organization = 'ABC';
		it('should successfully fetch repository records by organization', async () => {
			const config: LyricRepositoryConfig = {
				type: RepositoryType.LYRIC,
				name: 'Lyric Repository',
				categoryId: 123,
				code: 'lyricRepo1',
				baseUrl: 'http://localhost',
				validDataOnly: true,
				indexName: 'index_1_1',
			};

			// Mock response
			fetchMock.any({
				records: [
					{ id: 1, data: { name: 'test' } },
					{ id: 2, data: { name: 'test 2' } },
				],
			});

			const records: DataRecordNested[] = [];

			for await (const items of lyricRepository(config).getOrganizationRecords({ organization })) {
				records.push(...items);
			}

			expect(fetchMock.callHistory.calls().length).to.eql(1);
			expect(records.length).to.eql(2);
		});

		it('should successfully fetch repository records by organization with pagination', async () => {
			const config: LyricRepositoryConfig = {
				type: RepositoryType.LYRIC,
				name: 'Lyric Repository',
				categoryId: 123,
				code: 'lyricRepo1',
				baseUrl: 'http://localhost',
				validDataOnly: true,
				indexName: 'index_1_1',
				paginationSize: 2,
			};

			// Mock response
			const urlPage1 = 'http://localhost/data/category/123/organization/ABC?view=compound&pageSize=2&page=1';
			fetchMock.get(urlPage1, {
				records: [
					{ id: 1, data: { name: 'test' } },
					{ id: 2, data: { name: 'test 2' } },
				],
				pagination: {
					currentPage: 1,
					pageSize: 2,
					totalPages: 3,
					totalRecords: 5,
				},
			});
			const urlPage2 = 'http://localhost/data/category/123/organization/ABC?view=compound&pageSize=2&page=2';
			fetchMock.get(urlPage2, {
				records: [
					{ id: 3, data: { name: 'test 3' } },
					{ id: 4, data: { name: 'test 4' } },
				],
				pagination: {
					currentPage: 2,
					pageSize: 2,
					totalPages: 3,
					totalRecords: 5,
				},
			});
			const urlPage3 = 'http://localhost/data/category/123/organization/ABC?view=compound&pageSize=2&page=3';
			fetchMock.get(urlPage3, {
				records: [{ id: 5, data: { name: 'test 5' } }],
				pagination: {
					currentPage: 3,
					pageSize: 2,
					totalPages: 3,
					totalRecords: 5,
				},
			});

			const records: DataRecordNested[] = [];

			for await (const items of lyricRepository(config).getOrganizationRecords({ organization })) {
				records.push(...items);
			}

			expect(fetchMock.callHistory.calls().length).to.eql(3);
			expect(records.length).to.eql(5);
		});

		it('should return no records when receive different than 200 OK response', async () => {
			const config: LyricRepositoryConfig = {
				type: RepositoryType.LYRIC,
				name: 'Lyric Repository',
				categoryId: 123,
				code: 'lyricRepo1',
				baseUrl: 'http://localhost',
				validDataOnly: true,
				indexName: 'index_1_1',
				paginationSize: 100,
			};

			// Mock response
			fetchMock.any(401);

			const records: DataRecordNested[] = [];

			for await (const items of lyricRepository(config).getOrganizationRecords({ organization })) {
				records.push(...items);
			}

			expect(fetchMock.callHistory.calls().length).to.eql(1);
			expect(records.length).to.eql(0);
		});

		it('should throw an error if an error occcured', async () => {
			const config: LyricRepositoryConfig = {
				type: RepositoryType.LYRIC,
				name: 'Lyric Repository',
				categoryId: 123,
				code: 'lyricRepo1',
				baseUrl: 'http://localhost',
				validDataOnly: true,
				indexName: 'index_1_1',
				paginationSize: 100,
			};

			// Mock response
			fetchMock.any({ throws: { message: 'Network error' } });

			const records: DataRecordNested[] = [];

			try {
				for await (const items of lyricRepository(config).getOrganizationRecords({ organization })) {
					records.push(...items);
				}
			} catch (error) {
				expect(fetchMock.callHistory.calls().length).to.eql(1);
				expect(error.message).to.eql('Network error');
				expect(records.length).to.eql(0);
			}
		});
	});
	describe('getRecord', () => {
		const id = '999';
		const organization = 'ABC';
		it('should successfully fetch a record by ID', async () => {
			const config: LyricRepositoryConfig = {
				type: RepositoryType.LYRIC,
				name: 'Lyric Repository',
				categoryId: 123,
				code: 'lyricRepo1',
				baseUrl: 'http://localhost',
				validDataOnly: true,
				indexName: 'index_1_1',
			};

			// Mock response
			fetchMock.any({
				data: { id, data: { name: 'test 99' } },
				entityName: 'sample',
				isValid: true,
				organization: 'ABC',
				systemId: 'AEIOU123',
			});

			const record = await lyricRepository(config).getRecord({ id, organization });

			expect(fetchMock.callHistory.calls().length).to.eql(1);
			expect(record.data).to.eql({ id, data: { name: 'test 99' } });
			expect(record.entityName).to.eql('sample');
			expect(record.isValid).to.eql(true);
			expect(record.organization).to.eql('ABC');
			expect(record.id).to.eql('AEIOU123');
		});

		it('should return no records when receive different than 200 OK response', async () => {
			const config: LyricRepositoryConfig = {
				type: RepositoryType.LYRIC,
				name: 'Lyric Repository',
				categoryId: 123,
				code: 'lyricRepo1',
				baseUrl: 'http://localhost',
				validDataOnly: true,
				indexName: 'index_1_1',
				paginationSize: 100,
			};

			// Mock response
			fetchMock.any(401);

			const record = await lyricRepository(config).getRecord({ id, organization });

			expect(fetchMock.callHistory.calls().length).to.eql(1);
			expect(record).to.eql({});
		});

		it('should throw an error if an error occcured', async () => {
			const config: LyricRepositoryConfig = {
				type: RepositoryType.LYRIC,
				name: 'Lyric Repository',
				categoryId: 123,
				code: 'lyricRepo1',
				baseUrl: 'http://localhost',
				validDataOnly: true,
				indexName: 'index_1_1',
				paginationSize: 100,
			};

			// Mock response
			fetchMock.any({ throws: { message: 'Network error' } });

			let record: DataRecordNested = {};

			try {
				record = await lyricRepository(config).getRecord({ id, organization });
			} catch (error) {
				expect(fetchMock.callHistory.calls().length).to.eql(1);
				expect(error.message).to.eql('Network error');
				expect(record).to.eql({});
			}
		});
	});
});
