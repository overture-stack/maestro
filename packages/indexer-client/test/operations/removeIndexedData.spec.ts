// import { Client } from '@elastic/elasticsearch';
// import Mock from '@elastic/elasticsearch-mock';
// import type { WriteResponseBase } from '@elastic/elasticsearch/lib/api/types.js';
// import { expect } from 'chai';

// import { deleteData } from '../../src/sender/operations';

// describe('remove indexed Data', () => {
// 	const mock = new Mock();
// 	const client = new Client({
// 		node: 'http://localhost:9200',
// 		Connection: mock.getConnection(),
// 	});

// 	afterEach(() => {
// 		mock.clearAll();
// 	});

// 	it('should return `deleted` when doc is deleted', async () => {
// 		const mockIndex = 'test-index';
// 		const mockId = '1234';

// 		const mockResponse: WriteResponseBase = {
// 			_index: 'test_index',
// 			_id: '1',
// 			_version: 1,
// 			result: 'deleted',
// 			_shards: {
// 				total: 1,
// 				successful: 1,
// 				failed: 0,
// 			},
// 			_seq_no: 1,
// 			_primary_term: 1,
// 		};

// 		// Stub client.index to return a mock response on indexing new data
// 		mock.add(
// 			{
// 				method: 'DELETE',
// 				path: '/test-index/_doc/1234',
// 			},
// 			() => {
// 				return mockResponse;
// 			},
// 		);

// 		// Act
// 		const result = await deleteData(client as Client, mockIndex, mockId);
// 		expect(result.result).to.eql('deleted');
// 	});

// 	it('should return `not_found` when doc is deleted', async () => {
// 		const mockIndex = 'test-index';
// 		const mockId = '1234';

// 		const mockResponse: WriteResponseBase = {
// 			_index: 'test_index',
// 			_id: '1',
// 			_version: 1,
// 			result: 'not_found',
// 			_shards: {
// 				total: 1,
// 				successful: 1,
// 				failed: 0,
// 			},
// 			_seq_no: 1,
// 			_primary_term: 1,
// 		};

// 		// Stub client.index to return a mock response on indexing new data
// 		mock.add(
// 			{
// 				method: 'DELETE',
// 				path: '/test-index/_doc/1234',
// 			},
// 			() => {
// 				return mockResponse;
// 			},
// 		);

// 		// Act
// 		const result = await deleteData(client as Client, mockIndex, mockId);
// 		expect(result.result).to.eql('not_found');
// 	});
// });
