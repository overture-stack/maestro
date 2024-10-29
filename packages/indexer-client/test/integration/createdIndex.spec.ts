import { ElasticsearchContainer, StartedElasticsearchContainer } from '@testcontainers/elasticsearch';
import { expect } from 'chai';

import type { IElasticsearchService } from '@overture-stack/maestro-common';

import { es7 } from '../../src/client/v7/client.js';
import { es8 } from '../../src/client/v8/client.js';

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
		const clientVersion = this.ctx.clientVersion;

		// Initialize our client wrapper
		if (clientVersion === 7) {
			client = es7({ nodes: esHost, version: 7, basicAuth: { enabled: false } });
		} else if (clientVersion === 8) {
			client = es8({ nodes: esHost, version: 8, basicAuth: { enabled: false } });
		}

		// Wait for Elasticsearch to be ready
		await new Promise((resolve) => setTimeout(resolve, 5000)); // Add a delay to ensure Elasticsearch is ready
	});

	after(async () => {
		// Stop the container after tests are complete
		await container.stop();
	});

	it('should return false when index does not exist', async () => {
		const indexName = 'test-index';

		const result = await client.createIndex(indexName);
		expect(result).to.eql(false);
	});

	it('should return true when index already exists', async () => {
		const indexName = 'test-index';

		const result = await client.createIndex(indexName);

		expect(result).to.eql(true);
	});

	it('should return false when a ConnectionError is thrown', async () => {
		const indexName = 'test-index';

		// Setting an invalid node url to throw a Connection Error
		if (this.ctx.clientVersion === 7) {
			client = es7({ nodes: 'http://unknown', version: 7, basicAuth: { enabled: false } });
		} else if (this.ctx.clientVersion === 8) {
			client = es8({ nodes: 'http://unknown', version: 8, basicAuth: { enabled: false } });
		}

		const result = await client.createIndex(indexName);

		expect(result).to.eql(false);
	});
}
