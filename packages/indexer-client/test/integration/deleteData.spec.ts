import { ElasticsearchContainer, StartedElasticsearchContainer } from '@testcontainers/elasticsearch';
import { expect } from 'chai';

import { DataRecordNested, ElasticsearchService, ElasticSearchSupportedVersions } from '@overture-stack/maestro-common';

import { es7 } from '../../src/client/v7/client.js';
import { es8 } from '../../src/client/v8/client.js';

export default function suite() {
	let container: StartedElasticsearchContainer;
	let client: ElasticsearchService;

	before(async () => {
		// Start an Elasticsearch container
		container = await new ElasticsearchContainer(this.ctx.dockerImage)
			.withEnvironment({
				'xpack.security.enabled': 'false',
			})
			.start();

		// Get the connection details for the running container
		const esHost = container.getHttpUrl();
		const clientVersion = this.ctx.clientVersion;

		// Initialize our client wrapper
		if (clientVersion === ElasticSearchSupportedVersions.V7) {
			client = es7({ nodes: esHost, version: ElasticSearchSupportedVersions.V7, basicAuth: { enabled: false } });
		} else if (clientVersion === ElasticSearchSupportedVersions.V8) {
			client = es8({ nodes: esHost, version: ElasticSearchSupportedVersions.V8, basicAuth: { enabled: false } });
		}

		// Wait for Elasticsearch to be ready
		await new Promise((resolve) => setTimeout(resolve, 5000)); // Add a delay to ensure Elasticsearch is ready
	});

	after(async () => {
		// Stop the container after tests are complete
		await container.stop();
	});

	it('should return successful true when deleting data', async () => {
		const indexName = 'test-index';

		// insert data
		const mockData: DataRecordNested = {
			id: '1234',
			data: { key: 'value' },
			entityName: 'test-entity',
			organization: 'test-org',
		};

		await client.addData(indexName, mockData);

		// delete data
		const id = '1234';
		const result = await client.deleteData(indexName, id);

		expect(result.successful).to.eql(true);
		expect(result.indexName).to.eql(indexName);
		expect(Object.keys(result.failureData).length).to.eq(0);
	});

	it('should return successful false when deleting id does not exists', async () => {
		const indexName = 'test-index';

		const idDoesNotExists = '890';

		const result = await client.deleteData(indexName, idDoesNotExists);
		expect(result.successful).to.eql(false);
		expect(result.indexName).to.eql(indexName);
		expect(Object.keys(result.failureData).length).to.eq(1);
		expect(result.failureData).to.eql({ '890': ['ResponseError'] });
	});

	it('should return successful false when a ConnectionError is thrown', async () => {
		const indexName = 'test-index';

		// Setting an invalid node url to throw a Connection Error
		if (this.ctx.clientVersion === ElasticSearchSupportedVersions.V7) {
			client = es7({
				nodes: 'http://unknown',
				version: ElasticSearchSupportedVersions.V7,
				basicAuth: { enabled: false },
			});
		} else if (this.ctx.clientVersion === ElasticSearchSupportedVersions.V8) {
			client = es8({
				nodes: 'http://unknown',
				version: ElasticSearchSupportedVersions.V8,
				basicAuth: { enabled: false },
			});
		}

		const id = '123';
		const result = await client.deleteData(indexName, id);
		expect(result.successful).to.eql(false);
		expect(result.indexName).to.eql(indexName);
		expect(Object.keys(result.failureData).length).to.eq(1);
		expect(result.failureData).to.eql({ '123': ['ConnectionError'] });
	});
}
