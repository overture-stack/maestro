import { expect } from 'chai';
import fetchMock from 'fetch-mock';
import { describe, it } from 'mocha';

import { type DataRecordNested, RepositoryType, type SongRepositoryConfig } from '@overture-stack/maestro-common';

import { songRepository } from '../src/repositories/song/repository';

describe('Song Repository', () => {
	const studyId = 'ABC123';
	beforeEach(() => {
		fetchMock.mockGlobal();
	});

	afterEach(() => {
		fetchMock.hardReset();
	});
	describe('getRepositoryRecords', () => {
		it('should successfully fetch repository records', async () => {
			const config: SongRepositoryConfig = {
				type: RepositoryType.SONG,
				name: 'Song Repository',
				code: 'songRepo1',
				baseUrl: 'http://localhost',
				indexName: 'index_1_1',
				analysisCentricEnabled: true,
				indexableStudyStates: 'PUBLISHED',
			};

			// Mock response
			const allStudiesUrl = 'http://localhost/studies/all';
			fetchMock.get(allStudiesUrl, [studyId]);

			const repositoryUrl = `http://localhost/studies/${studyId}/analysis`;
			const analysisMock1 = {
				analysisId: 'AAA-BBBB-CCCCC',
				analysisState: 'PUBLISHED',
				samples: [
					{
						donor: {
							donorId: 'DODODODO',
						},
						sampleId: 'SASASASA',
						specimen: {
							specimenId: 'SPSPSPSPSPS',
						},
						specimenId: 'SPSPSPSPSPS',
						submitterSampleId: 'SUSUSUSUSU',
					},
				],
				studyId: 'ABC123',
				updatedAt: '2025-01-08T12:33:53.323Z',
			};
			const analysisMock2 = {
				analysisId: 'DDD-EEEE-FFFFF',
				analysisState: 'PUBLISHED',
				samples: [
					{
						donor: { donorId: 'ZZZZZZZZ' },
						sampleId: 'BBBBBBBB',
						specimen: { specimenId: 'XYZXYZXYZ' },
						specimenId: 'XYZXYZXYZ',
						submitterSampleId: 'TESTTESTTEST',
					},
				],
				studyId: 'XYZ987',
				updatedAt: '2025-01-09T15:20:00.000Z',
			};
			fetchMock.get(repositoryUrl, [analysisMock1, analysisMock2]);

			const records: DataRecordNested[] = [];

			for await (const items of songRepository(config).getRepositoryRecords()) {
				records.push(...items);
			}

			expect(fetchMock.callHistory.calls().length).to.eql(2);
			expect(records.length).to.eql(2);
			expect(records).to.eql([
				{ ...analysisMock1, _id: 'AAA-BBBB-CCCCC' },
				{ ...analysisMock2, _id: 'DDD-EEEE-FFFFF' },
			]);
		});

		it('should successfully fetch repository records with pagination', async () => {
			const config: SongRepositoryConfig = {
				type: RepositoryType.SONG,
				name: 'Song Repository',
				code: 'songRepo1',
				baseUrl: 'http://localhost',
				indexName: 'index_1_1',
				paginationSize: 2,
				analysisCentricEnabled: true,
				indexableStudyStates: 'PUBLISHED',
			};

			// Mock response
			const allStudiesUrl = 'http://localhost/studies/all';
			fetchMock.get(allStudiesUrl, [studyId]);

			const urlPage1 = `http://localhost/studies/${studyId}/analysis/paginated?limit=2&offset=0`;
			const analysisMock1 = {
				analysisId: 'AAA-BBBB-CCCCC',
				analysisState: 'PUBLISHED',
				samples: [
					{
						donor: {
							donorId: 'DODODODO',
						},
						sampleId: 'SASASASA',
						specimen: {
							specimenId: 'SPSPSPSPSPS',
						},
						specimenId: 'SPSPSPSPSPS',
						submitterSampleId: 'SUSUSUSUSU',
					},
				],
				studyId: 'ABC123',
				updatedAt: '2025-01-08T12:33:53.323Z',
			};
			const analysisMock2 = {
				analysisId: 'DDD-EEEE-FFFFF',
				analysisState: 'PUBLISHED',
				samples: [
					{
						donor: { donorId: 'ZZZZZZZZ' },
						sampleId: 'BBBBBBBB',
						specimen: { specimenId: 'XYZXYZXYZ' },
						specimenId: 'XYZXYZXYZ',
						submitterSampleId: 'TESTTESTTEST',
					},
				],
				studyId: 'XYZ987',
				updatedAt: '2025-01-09T15:20:00.000Z',
			};
			fetchMock.get(urlPage1, {
				analyses: [analysisMock1, analysisMock2],
				currentTotalAnalyses: 2,
				totalAnalyses: 4,
			});

			const urlPage2 = `http://localhost/studies/${studyId}/analysis/paginated?limit=2&offset=1`;
			const analysisMock3 = {
				analysisId: 'GGG-HHHH-IIIII',
				analysisState: 'PUBLISHED',
				samples: [
					{
						donor: { donorId: 'YYYYYYYY' },
						sampleId: 'CCCCCCCC',
						specimen: { specimenId: 'LMNLMNLMN' },
						specimenId: 'LMNLMNLMN',
						submitterSampleId: 'SAMPLE-SAMPLE',
					},
				],
				studyId: 'DEF456',
				updatedAt: '2025-01-10T16:45:00.000Z',
			};
			const analysisMock4 = {
				analysisId: 'JJJ-KKKK-LLLLL',
				analysisState: 'PUBLISHED',
				samples: [
					{
						donor: { donorId: 'WWWWWWWW' },
						sampleId: 'DDDDDDDD',
						specimen: { specimenId: 'ABCABCABC' },
						specimenId: 'ABCABCABC',
						submitterSampleId: 'FINAL-SAMPLE',
					},
				],
				studyId: 'XYZ654',
				updatedAt: '2025-01-12T17:50:00.000Z',
			};
			fetchMock.get(urlPage2, {
				analyses: [analysisMock3, analysisMock4],
				currentTotalAnalyses: 4,
				totalAnalyses: 4,
			});

			const records: DataRecordNested[] = [];

			for await (const items of songRepository(config).getRepositoryRecords()) {
				records.push(...items);
			}

			expect(fetchMock.callHistory.calls().length).to.eql(3);
			expect(records.length).to.eql(4);
			expect(records).to.eql([
				{ ...analysisMock1, _id: 'AAA-BBBB-CCCCC' },
				{ ...analysisMock2, _id: 'DDD-EEEE-FFFFF' },
				{ ...analysisMock3, _id: 'GGG-HHHH-IIIII' },
				{ ...analysisMock4, _id: 'JJJ-KKKK-LLLLL' },
			]);
		});

		it('should return no records when receive different than 200 OK response', async () => {
			const config: SongRepositoryConfig = {
				type: RepositoryType.SONG,
				name: 'Song Repository',
				code: 'songRepo1',
				baseUrl: 'http://localhost',
				indexName: 'index_1_1',
				analysisCentricEnabled: true,
				indexableStudyStates: 'PUBLISHED',
			};

			// Mock response
			fetchMock.any(401);

			const records: DataRecordNested[] = [];

			for await (const items of songRepository(config).getRepositoryRecords()) {
				records.push(...items);
			}

			expect(fetchMock.callHistory.calls().length).to.eql(1);
			expect(records.length).to.eql(0);
		});

		it('should throw an error if an error occcured', async () => {
			const config: SongRepositoryConfig = {
				type: RepositoryType.SONG,
				name: 'Song Repository',
				code: 'songRepo1',
				baseUrl: 'http://localhost',
				indexName: 'index_1_1',
				analysisCentricEnabled: true,
				indexableStudyStates: 'PUBLISHED',
			};

			// Mock response
			fetchMock.any({ throws: { message: 'Network error' } });

			const records: DataRecordNested[] = [];

			try {
				for await (const items of songRepository(config).getRepositoryRecords()) {
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
		it('should successfully fetch repository records by organization', async () => {
			const config: SongRepositoryConfig = {
				type: RepositoryType.SONG,
				name: 'Song Repository',
				code: 'songRepo1',
				baseUrl: 'http://localhost',
				indexName: 'index_1_1',
				analysisCentricEnabled: true,
				indexableStudyStates: 'PUBLISHED',
			};

			// Mock response
			const analysisMock1 = {
				analysisId: 'AAA-BBBB-CCCCC',
				analysisState: 'PUBLISHED',
				samples: [
					{
						donor: {
							donorId: 'DODODODO',
						},
						sampleId: 'SASASASA',
						specimen: {
							specimenId: 'SPSPSPSPSPS',
						},
						specimenId: 'SPSPSPSPSPS',
						submitterSampleId: 'SUSUSUSUSU',
					},
				],
				studyId: 'ABC123',
				updatedAt: '2025-01-08T12:33:53.323Z',
			};
			const analysisMock2 = {
				analysisId: 'DDD-EEEE-FFFFF',
				analysisState: 'PUBLISHED',
				samples: [
					{
						donor: { donorId: 'ZZZZZZZZ' },
						sampleId: 'BBBBBBBB',
						specimen: { specimenId: 'XYZXYZXYZ' },
						specimenId: 'XYZXYZXYZ',
						submitterSampleId: 'TESTTESTTEST',
					},
				],
				studyId: 'XYZ987',
				updatedAt: '2025-01-09T15:20:00.000Z',
			};
			fetchMock.any([analysisMock1, analysisMock2]);

			const records: DataRecordNested[] = [];

			for await (const items of songRepository(config).getOrganizationRecords({ organization: studyId })) {
				records.push(...items);
			}

			expect(fetchMock.callHistory.calls().length).to.eql(1);
			expect(records.length).to.eql(2);
			expect(records).to.eql([
				{ ...analysisMock1, _id: 'AAA-BBBB-CCCCC' },
				{ ...analysisMock2, _id: 'DDD-EEEE-FFFFF' },
			]);
		});

		it('should successfully fetch repository records by organization with pagination', async () => {
			const config: SongRepositoryConfig = {
				type: RepositoryType.SONG,
				name: 'Song Repository',
				code: 'songRepo1',
				baseUrl: 'http://localhost',
				indexName: 'index_1_1',
				paginationSize: 2,
				analysisCentricEnabled: true,
				indexableStudyStates: 'PUBLISHED',
			};

			// Mock response
			const urlPage1 = `http://localhost/studies/${studyId}/analysis/paginated?limit=2&offset=0`;
			const analysisMock1 = {
				analysisId: 'AAA-BBBB-CCCCC',
				analysisState: 'PUBLISHED',
				samples: [
					{
						donor: {
							donorId: 'DODODODO',
						},
						sampleId: 'SASASASA',
						specimen: {
							specimenId: 'SPSPSPSPSPS',
						},
						specimenId: 'SPSPSPSPSPS',
						submitterSampleId: 'SUSUSUSUSU',
					},
				],
				studyId: 'ABC123',
				updatedAt: '2025-01-08T12:33:53.323Z',
			};
			const analysisMock2 = {
				analysisId: 'DDD-EEEE-FFFFF',
				analysisState: 'PUBLISHED',
				samples: [
					{
						donor: { donorId: 'ZZZZZZZZ' },
						sampleId: 'BBBBBBBB',
						specimen: { specimenId: 'XYZXYZXYZ' },
						specimenId: 'XYZXYZXYZ',
						submitterSampleId: 'TESTTESTTEST',
					},
				],
				studyId: 'XYZ987',
				updatedAt: '2025-01-09T15:20:00.000Z',
			};
			fetchMock.get(urlPage1, {
				analyses: [analysisMock1, analysisMock2],
				currentTotalAnalyses: 2,
				totalAnalyses: 4,
			});
			const urlPage2 = `http://localhost/studies/${studyId}/analysis/paginated?limit=2&offset=1`;
			const analysisMock3 = {
				analysisId: 'GGG-HHHH-IIIII',
				analysisState: 'PUBLISHED',
				samples: [
					{
						donor: { donorId: 'YYYYYYYY' },
						sampleId: 'CCCCCCCC',
						specimen: { specimenId: 'LMNLMNLMN' },
						specimenId: 'LMNLMNLMN',
						submitterSampleId: 'SAMPLE-SAMPLE',
					},
				],
				studyId: 'DEF456',
				updatedAt: '2025-01-10T16:45:00.000Z',
			};
			const analysisMock4 = {
				analysisId: 'JJJ-KKKK-LLLLL',
				analysisState: 'PUBLISHED',
				samples: [
					{
						donor: { donorId: 'WWWWWWWW' },
						sampleId: 'DDDDDDDD',
						specimen: { specimenId: 'ABCABCABC' },
						specimenId: 'ABCABCABC',
						submitterSampleId: 'FINAL-SAMPLE',
					},
				],
				studyId: 'XYZ654',
				updatedAt: '2025-01-12T17:50:00.000Z',
			};
			fetchMock.get(urlPage2, {
				analyses: [analysisMock3, analysisMock4],
				currentTotalAnalyses: 4,
				totalAnalyses: 4,
			});

			const records: DataRecordNested[] = [];

			for await (const items of songRepository(config).getOrganizationRecords({ organization: studyId })) {
				records.push(...items);
			}

			expect(fetchMock.callHistory.calls().length).to.eql(2);
			expect(records.length).to.eql(4);
			expect(records).to.eql([
				{ ...analysisMock1, _id: 'AAA-BBBB-CCCCC' },
				{ ...analysisMock2, _id: 'DDD-EEEE-FFFFF' },
				{ ...analysisMock3, _id: 'GGG-HHHH-IIIII' },
				{ ...analysisMock4, _id: 'JJJ-KKKK-LLLLL' },
			]);
		});

		it('should return no records when receive different than 200 OK response', async () => {
			const config: SongRepositoryConfig = {
				type: RepositoryType.SONG,
				name: 'Song Repository',
				code: 'songRepo1',
				baseUrl: 'http://localhost',
				indexName: 'index_1_1',
				analysisCentricEnabled: true,
				indexableStudyStates: 'PUBLISHED',
			};

			// Mock response
			fetchMock.any(401);

			const records: DataRecordNested[] = [];

			for await (const items of songRepository(config).getOrganizationRecords({ organization: studyId })) {
				records.push(...items);
			}

			expect(fetchMock.callHistory.calls().length).to.eql(1);
			expect(records.length).to.eql(0);
		});

		it('should throw an error if an error occcured', async () => {
			const config: SongRepositoryConfig = {
				type: RepositoryType.SONG,
				name: 'Song Repository',
				code: 'songRepo1',
				baseUrl: 'http://localhost',
				indexName: 'index_1_1',
				analysisCentricEnabled: true,
				indexableStudyStates: 'PUBLISHED',
			};

			// Mock response
			fetchMock.any({ throws: { message: 'Network error' } });

			const records: DataRecordNested[] = [];

			try {
				for await (const items of songRepository(config).getOrganizationRecords({ organization: studyId })) {
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
		const id = 'AAA-BBBB-CCCCC';
		it('should successfully fetch a record by ID', async () => {
			const config: SongRepositoryConfig = {
				type: RepositoryType.SONG,
				name: 'Song Repository',
				code: 'songRepo1',
				baseUrl: 'http://localhost',
				indexName: 'index_1_1',
				analysisCentricEnabled: true,
				indexableStudyStates: 'PUBLISHED',
			};

			// Mock response
			const urlMock = `http://localhost/studies/${studyId}/analysis/${id}`;
			const analysisMock1 = {
				analysisId: 'AAA-BBBB-CCCCC',
				analysisState: 'PUBLISHED',
				samples: [
					{
						donor: {
							donorId: 'DODODODO',
						},
						sampleId: 'SASASASA',
						specimen: {
							specimenId: 'SPSPSPSPSPS',
						},
						specimenId: 'SPSPSPSPSPS',
						submitterSampleId: 'SUSUSUSUSU',
					},
				],
				studyId: 'ABC123',
				updatedAt: '2025-01-08T12:33:53.323Z',
			};
			fetchMock.get(urlMock, analysisMock1);

			const record = await songRepository(config).getRecord({ id, organization: studyId });

			expect(fetchMock.callHistory.calls().length).to.eql(1);
			expect(record).to.eql({ ...analysisMock1, _id: 'AAA-BBBB-CCCCC' });
		});

		it('should return no records when receive different than 200 OK response', async () => {
			const config: SongRepositoryConfig = {
				type: RepositoryType.SONG,
				name: 'Song Repository',
				code: 'songRepo1',
				baseUrl: 'http://localhost',
				indexName: 'index_1_1',
				analysisCentricEnabled: true,
				indexableStudyStates: 'PUBLISHED',
			};

			// Mock response
			fetchMock.any(401);

			const record = await songRepository(config).getRecord({ id, organization: studyId });

			expect(fetchMock.callHistory.calls().length).to.eql(1);
			expect(record).to.eql({});
		});

		it('should throw an error if an error occcured', async () => {
			const config: SongRepositoryConfig = {
				type: RepositoryType.SONG,
				name: 'Song Repository',
				code: 'songRepo1',
				baseUrl: 'http://localhost',
				indexName: 'index_1_1',
				analysisCentricEnabled: true,
				indexableStudyStates: 'PUBLISHED',
			};

			// Mock response
			fetchMock.any({ throws: { message: 'Network error' } });

			let record: DataRecordNested = {};

			try {
				record = await songRepository(config).getRecord({ id, organization: studyId });
			} catch (error) {
				expect(fetchMock.callHistory.calls().length).to.eql(1);
				expect(error.message).to.eql('Network error');
				expect(record).to.eql({});
			}
		});
	});
});
