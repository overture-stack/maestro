import { ElasticsearchContainer, StartedElasticsearchContainer } from '@testcontainers/elasticsearch';
import { expect } from 'chai';

import type { IElasticsearchService } from '@overture-stack/maestro-common';

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

	it('should return successful true when indexing server is available', async () => {
		const result = await client.ping();
		expect(result).to.eql(true);
	});

	it('should return successful false when indexing server throws an error', async () => {
		// Setting an invalid node url to throw a Connection Error
		if (this.ctx.clientVersion === 7) {
			client = es7('http://unknown');
		} else if (this.ctx.clientVersion === 8) {
			client = es8('http://unknown');
		}

		const result = await client.ping();
		expect(result).to.eql(false);
	});
}
