import { expect } from 'chai';

import { es7 } from '../src/client/v7/client.js';
import { es8 } from '../src/client/v8/client.js';

describe('Initialize Client', () => {
	describe('Client V7', () => {
		it('should provide indexing functions', () => {
			const nodeUrl = 'http://myserver:9200';
			const client = es7({ nodes: nodeUrl, basicAuth: { enabled: false }, version: 7 });

			expect(client).to.have.property('createIndex');
			expect(client).to.have.property('addData');
			expect(client).to.have.property('updateData');
			expect(client).to.have.property('deleteData');
			expect(client).to.have.property('ping');
		});
	});
	describe('Client V8', () => {
		it('should provide indexing functions', () => {
			const nodeUrl = 'http://myserver:9200';
			const client = es8({ nodes: nodeUrl, basicAuth: { enabled: false }, version: 8 });

			expect(client).to.have.property('createIndex');
			expect(client).to.have.property('addData');
			expect(client).to.have.property('updateData');
			expect(client).to.have.property('deleteData');
			expect(client).to.have.property('ping');
		});
	});
});
