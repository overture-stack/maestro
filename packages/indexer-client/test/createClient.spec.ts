import { expect } from 'chai';

import { ElasticSearchSupportedVersions } from '@overture-stack/maestro-common';

import { es7 } from '../src/client/v7/client.js';
import { es8 } from '../src/client/v8/client.js';
import { clientProvider } from '../src/index.js';

describe('Initialize Indexer', () => {
	describe('Provider', () => {
		it('should create a provider V7 with indexing functions', () => {
			const nodeUrl = 'http://myserver:9200';
			const providerConfig = {
				nodes: nodeUrl,
				basicAuth: { enabled: false },
				version: ElasticSearchSupportedVersions.V7,
			};
			const provider = clientProvider(providerConfig);
			expect(provider).to.have.property('createIndex');
			expect(provider).to.have.property('addData');
			expect(provider).to.have.property('updateData');
			expect(provider).to.have.property('deleteData');
			expect(provider).to.have.property('ping');
		});
		it('should create a provider V8 with indexing functions', () => {
			const nodeUrl = 'http://myserver:9200';
			const providerConfig = {
				nodes: nodeUrl,
				basicAuth: { enabled: false },
				version: ElasticSearchSupportedVersions.V8,
			};
			const provider = clientProvider(providerConfig);
			expect(provider).to.have.property('createIndex');
			expect(provider).to.have.property('addData');
			expect(provider).to.have.property('updateData');
			expect(provider).to.have.property('deleteData');
			expect(provider).to.have.property('ping');
		});
		it('should throw an error if configuration version is incorrect', () => {
			try {
				const nodeUrl = 'http://myserver:9200';
				const providerConfig = { nodes: nodeUrl, basicAuth: { enabled: false }, version: 10 };
				clientProvider(providerConfig);
			} catch (error) {
				expect(error.message).to.eql('Unsupported Elasticsearch version');
			}
		});
	});
	describe('Client', () => {
		it('should create a client V7 with indexing functions', () => {
			const nodeUrl = 'http://myserver:9200';
			const client = es7({ nodes: nodeUrl, basicAuth: { enabled: false }, version: ElasticSearchSupportedVersions.V7 });

			expect(client).to.have.property('createIndex');
			expect(client).to.have.property('addData');
			expect(client).to.have.property('updateData');
			expect(client).to.have.property('deleteData');
			expect(client).to.have.property('ping');
		});
		it('should create a client V8 with indexing functions', () => {
			const nodeUrl = 'http://myserver:9200';
			const client = es8({ nodes: nodeUrl, basicAuth: { enabled: false }, version: ElasticSearchSupportedVersions.V8 });

			expect(client).to.have.property('createIndex');
			expect(client).to.have.property('addData');
			expect(client).to.have.property('updateData');
			expect(client).to.have.property('deleteData');
			expect(client).to.have.property('ping');
		});
		it('should throw an error if client V7 configuration version is incorrect', () => {
			const nodeUrl = 'http://myserver:9200';

			try {
				es7({ nodes: nodeUrl, basicAuth: { enabled: false }, version: 10 });
			} catch (error) {
				expect(error.message).to.eql('Invalid Client Configuration');
			}
		});
		it('should throw an error if client V8 configuration version is incorrect', () => {
			const nodeUrl = 'http://myserver:9200';

			try {
				es8({ nodes: nodeUrl, basicAuth: { enabled: false }, version: 10 });
			} catch (error) {
				expect(error.message).to.eql('Invalid Client Configuration');
			}
		});
	});
});
