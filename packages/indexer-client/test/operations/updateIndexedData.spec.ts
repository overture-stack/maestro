// import { Client, errors } from '@elastic/elasticsearch';
// import Mock from '@elastic/elasticsearch-mock';
// import type { WriteResponseBase } from '@elastic/elasticsearch/lib/api/types.js';
// import { expect } from 'chai';

// import { deleteData, updateData } from '../../src/sender/operations';
// import type { IndexData } from '../../src/sender/service';
// import type { DataRecordValue } from '../../src/types/dataRecord';

// describe('remove indexed Data', () => {
// 	const mock = new Mock();
// 	const client = new Client({
// 		node: 'http://localhost:9200',
// 		Connection: mock.getConnection(),
// 	});

// 	afterEach(() => {
// 		mock.clearAll();
// 	});

// 	it('should return `updated` when doc is updated', async () => {
// 		const mockIndex = 'test-index';
// 		const mockData: { id: string; data: Record<string, DataRecordValue> } = {
// 			id: '1234',
// 			data: { key: 'value' },
// 		};

// 		const mockResponse: WriteResponseBase = {
// 			_index: 'test_index',
// 			_id: '1',
// 			_version: 2,
// 			result: 'updated',
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
// 				method: 'POST',
// 				path: '/test-index/_update/1234',
// 			},
// 			() => {
// 				return mockResponse;
// 			},
// 		);

// 		const result = await updateData(client as Client, mockIndex, mockData);
// 		expect(result.result).to.eql('updated');
// 	});

// 	it('should return `not_found` when doc id does not exists', async () => {
// 		const mockIndex = 'test-index';
// 		const mockData: { id: string; data: Record<string, DataRecordValue> } = {
// 			id: '456',
// 			data: { key: 'value' },
// 		};

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
// 				method: 'POST',
// 				path: '/test-index/_update/456',
// 			},
// 			() => {
// 				return mockResponse;
// 			},
// 		);

// 		const result = await updateData(client as Client, mockIndex, mockData);
// 		expect(result.result).to.eql('not_found');
// 	});
// });
