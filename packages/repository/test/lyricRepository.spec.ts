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
					{ systemId: 1, data: { name: 'test' } },
					{ systemId: 2, data: { name: 'test 2' } },
				],
			});

			const records: DataRecordNested[] = [];

			for await (const items of lyricRepository(config).getRepositoryRecords()) {
				records.push(...items);
			}

			expect(fetchMock.callHistory.calls().length).to.eql(1);
			expect(records.length).to.eql(2);
			expect(records).to.eql([
				{ systemId: 1, data: { name: 'test' }, _id: 1 },
				{ systemId: 2, data: { name: 'test 2' }, _id: 2 },
			]);
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
					{ systemId: 1, data: { name: 'test' } },
					{ systemId: 2, data: { name: 'test 2' } },
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
					{ systemId: 3, data: { name: 'test 3' } },
					{ systemId: 4, data: { name: 'test 4' } },
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
				records: [{ systemId: 5, data: { name: 'test 5' } }],
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
			expect(records).eql([
				{ systemId: 1, data: { name: 'test' }, _id: 1 },
				{ systemId: 2, data: { name: 'test 2' }, _id: 2 },
				{ systemId: 3, data: { name: 'test 3' }, _id: 3 },
				{ systemId: 4, data: { name: 'test 4' }, _id: 4 },
				{ systemId: 5, data: { name: 'test 5' }, _id: 5 },
			]);
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
					{ systemId: 1, data: { name: 'test' } },
					{ systemId: 2, data: { name: 'test 2' } },
				],
			});

			const records: DataRecordNested[] = [];

			for await (const items of lyricRepository(config).getOrganizationRecords({ organization })) {
				records.push(...items);
			}

			expect(fetchMock.callHistory.calls().length).to.eql(1);
			expect(records.length).to.eql(2);
			expect(records).eql([
				{ systemId: 1, data: { name: 'test' }, _id: 1 },
				{ systemId: 2, data: { name: 'test 2' }, _id: 2 },
			]);
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
					{ systemId: 1, data: { name: 'test' } },
					{ systemId: 2, data: { name: 'test 2' } },
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
					{ systemId: 3, data: { name: 'test 3' } },
					{ systemId: 4, data: { name: 'test 4' } },
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
				records: [{ systemId: 5, data: { name: 'test 5' } }],
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
			expect(records).eql([
				{ systemId: 1, data: { name: 'test' }, _id: 1 },
				{ systemId: 2, data: { name: 'test 2' }, _id: 2 },
				{ systemId: 3, data: { name: 'test 3' }, _id: 3 },
				{ systemId: 4, data: { name: 'test 4' }, _id: 4 },
				{ systemId: 5, data: { name: 'test 5' }, _id: 5 },
			]);
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
		const systemId = 'AEIOU123';
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
				data: { name: 'test 99' },
				entityName: 'sample',
				isValid: true,
				organization: 'ABC',
				systemId,
			});

			const record = await lyricRepository(config).getRecord({ id: systemId, organization });

			expect(fetchMock.callHistory.calls().length).to.eql(1);
			console.log(`HERE!!!:${JSON.stringify(record)}`);
			expect(record.data).to.eql({ name: 'test 99' });
			expect(record.entityName).to.eql('sample');
			expect(record.isValid).to.eql(true);
			expect(record.organization).to.eql('ABC');
			expect(record.systemId).to.eql('AEIOU123');
			expect(record._id).to.eql('AEIOU123');
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

			const record = await lyricRepository(config).getRecord({ id: systemId, organization });

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
				record = await lyricRepository(config).getRecord({ id: systemId, organization });
			} catch (error) {
				expect(fetchMock.callHistory.calls().length).to.eql(1);
				expect(error.message).to.eql('Network error');
				expect(record).to.eql({});
			}
		});
	});
});
