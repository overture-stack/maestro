// import { Client } from '@elastic/elasticsearch';
// import Mock from '@elastic/elasticsearch-mock';
// import { expect } from 'chai';

// import { NotFoundErrorResponse, OkResponse } from 'indexer-provider/test/fixed/httpResponses';
// import { createIndexIfNotExists } from '../../src/sender/operations';

// describe('create Index', () => {
// 	const mock = new Mock();
// 	const client = new Client({
// 		node: 'http://localhost:9200',
// 		Connection: mock.getConnection(),
// 	});

// 	afterEach(() => {
// 		mock.clearAll();
// 	});

// 	it('should return false when index does not exist', async () => {
// 		const mockIndex = 'test-index';

// 		// Mock index does not exist
// 		mock.add(
// 			{
// 				method: 'HEAD',
// 				path: '*',
// 			},
// 			() => NotFoundErrorResponse,
// 		);

// 		// Mock index creation
// 		mock.add(
// 			{
// 				method: 'PUT',
// 				path: '/test-index',
// 			},
// 			() => {
// 				return {
// 					acknowledged: true,
// 					shards_acknowledged: true,
// 					index: 'test-index',
// 				};
// 			},
// 		);

// 		const result = await createIndexIfNotExists(client as Client, mockIndex);
// 		expect(result).to.eql(false);
// 	});

// 	it('should return true when index does not exist', async () => {
// 		const mockIndex = 'test-index';

// 		// Mock index does not exist
// 		mock.add(
// 			{
// 				method: 'HEAD',
// 				path: '*',
// 			},
// 			() => OkResponse,
// 		);

// 		const result = await createIndexIfNotExists(client as Client, mockIndex);
// 		expect(result).to.eql(true);
// 	});
// });
