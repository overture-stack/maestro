// import { Client } from '@elastic/elasticsearch';
// import Mock from '@elastic/elasticsearch-mock';
// import type { WriteResponseBase } from '@elastic/elasticsearch/lib/api/types.js';
// import { expect } from 'chai';

// import { indexData } from '../../src/sender/operations';
// import { IndexData } from '../../src/sender/service';

// describe('indexData', () => {
// 	const mock = new Mock();
// 	const client = new Client({
// 		node: 'http://localhost:9200',
// 		Connection: mock.getConnection(),
// 	});

// 	afterEach(() => {
// 		mock.clearAll();
// 	});

// 	it('should return success result indexing data', async () => {
// 		const mockIndex = 'test-index';
// 		const mockData: IndexData = {
// 			id: '1234',
// 			data: { key: 'value' },
// 			entityName: 'test-entity',
// 			organization: 'test-org',
// 		};

// 		const mockResponse: WriteResponseBase = {
// 			_index: 'test-index',
// 			_id: '1234',
// 			_version: 1,
// 			result: 'created',
// 			_shards: { total: 1, successful: 1, failed: 0 },
// 			_seq_no: 1,
// 			_primary_term: 1,
// 		};

// 		// Stub client.index to return a mock response on indexing new data
// 		mock.add(
// 			{
// 				method: 'PUT',
// 				path: '/test-index/_doc/1234',
// 			},
// 			() => {
// 				return mockResponse;
// 			},
// 		);

// 		// Act
// 		const result = await indexData(client as Client, mockIndex, mockData);
// 		expect(result.result).to.eql('created');
// 	});
// });
