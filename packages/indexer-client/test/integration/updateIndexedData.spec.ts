import { ElasticsearchContainer, StartedElasticsearchContainer } from '@testcontainers/elasticsearch';
import { expect } from 'chai';

import type { DataRecordValue, IElasticsearchService, IndexData } from '@overture-stack/maestro-common';

import { es7, es8 } from '../../src/index.js';

export default function suite() {
	let container: StartedElasticsearchContainer;
	let client: IElasticsearchService;

	before(async () => {
		// Start an Elasticsearch container
		container = await new ElasticsearchContainer(this.ctx.dockerImage)
			.withEnvironment({
				'xpack.security.enabled': 'false',
			})
			.start();

		// Get the connection details for the running container
		const esHost = container.getHttpUrl();

		// Initialize our client wrapper
		if (this.ctx.clientVersion === 7) {
			client = es7(esHost);
		} else if (this.ctx.clientVersion === 8) {
			client = es8(esHost);
		}

		// Wait for Elasticsearch to be ready
		await new Promise((resolve) => setTimeout(resolve, 5000)); // Add a delay to ensure Elasticsearch is ready
	});

	after(async () => {
		// Stop the container after tests are complete
		await container.stop();
	});

	it('should return successful true when updating indexed data', async () => {
		const indexName = 'test-index';

		// Insert Data
		const insertData: IndexData = {
			id: '1234',
			data: { key: 'value' },
			entityName: 'test-entity',
			organization: 'test-org',
		};
		await client.indexData(indexName, insertData);

		// Edit Data
		const id = '1234';
		const editData: Record<string, DataRecordValue> = { key2: 'value2' };

		const result = await client.updateData(indexName, id, editData);
		expect(result.successful).to.eql(true);
		expect(result.indexName).to.eql(indexName);
		expect(Object.keys(result.failureData).length).to.eq(0);
	});

	it('should return successful false when doc id does not exists', async () => {
		const indexName = 'test-index';

		// Edit Data
		const id = '456';
		const editData: Record<string, DataRecordValue> = { key2: 'value2' };

		const result = await client.updateData(indexName, id, editData);
		expect(result.successful).to.eql(false);
		expect(result.indexName).to.eql(indexName);
		expect(Object.keys(result.failureData).length).to.eq(1);
		expect(result.failureData).to.eql({ '456': ['ResponseError'] });
	});

	it('should return successful false when a ConnectionError is thrown', async () => {
		const indexName = 'test-index';

		// Setting an invalid node url to throw a Connection Error
		if (this.ctx.clientVersion === 7) {
			client = es7('http://unknown');
		} else if (this.ctx.clientVersion === 8) {
			client = es8('http://unknown');
		}

		const id = '1234';
		const editData: Record<string, DataRecordValue> = { key2: 'value2' };

		const result = await client.updateData(indexName, id, editData);
		expect(result.successful).to.eql(false);
		expect(result.indexName).to.eql(indexName);
		expect(Object.keys(result.failureData).length).to.eq(1);
		expect(result.failureData).to.eql({ '1234': ['ConnectionError'] });
	});
}
