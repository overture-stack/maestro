import { ElasticsearchContainer, type StartedElasticsearchContainer } from '@testcontainers/elasticsearch';
import { expect } from 'chai';

import {
	DOCUMENT_NOT_FOUND,
	type DocumentFieldValue,
	type ElasticsearchService,
	ElasticSearchSupportedVersions,
	type ScanDocumentsOptions,
} from '@overture-stack/maestro-common';

import { es7 } from '../../src/client/v7/client.js';
import { es8 } from '../../src/client/v8/client.js';
import { recordLogs } from '../../testing/peripherals/recordingLogger.js';

export default function suite(this: Mocha.Suite) {
	let container: StartedElasticsearchContainer;
	let client: ElasticsearchService;

	const listDocuments = (
		index: string,
		options: ScanDocumentsOptions,
	): AsyncGenerator<DocumentFieldValue[], void, unknown> => {
		if (client.scanDocuments) {
			return client.scanDocuments(index, options);
		}
		throw new Error('The client under test lists no documents');
	};

	const scan = async (index: string, options: ScanDocumentsOptions): Promise<DocumentFieldValue[]> =>
		(await Array.fromAsync(listDocuments(index, options))).flat();

	const countOpenScrolls = async (): Promise<number> => {
		const response = await fetch(`${container.getHttpUrl()}/_nodes/stats/indices/search`);
		const stats: { nodes: Record<string, { indices: { search: { scroll_current: number } } }> } = await response.json();
		return Object.values(stats.nodes).reduce((total, node) => total + node.indices.search.scroll_current, 0);
	};

	const byDocumentId = (left: DocumentFieldValue, right: DocumentFieldValue): number =>
		left.documentId.localeCompare(right.documentId);

	const createIndexMappingOrganizationAsKeyword = async (index: string): Promise<void> => {
		const response = await fetch(`${container.getHttpUrl()}/${index}`, {
			body: JSON.stringify({ mappings: { properties: { organization: { type: 'keyword' } } } }),
			headers: { 'Content-Type': 'application/json' },
			method: 'PUT',
		});
		if (response.ok) {
			return;
		}
		throw new Error(`Creating index '${index}' answered ${response.status}`);
	};

	before(async () => {
		container = await new ElasticsearchContainer(this.ctx.dockerImage)
			.withEnvironment({
				'xpack.security.enabled': 'false',
			})
			.start();

		const esHost = container.getHttpUrl();
		const clientVersion = this.ctx.clientVersion;

		if (clientVersion === ElasticSearchSupportedVersions.V7) {
			client = es7({ nodes: esHost, version: ElasticSearchSupportedVersions.V7, basicAuth: { enabled: false } });
		} else if (clientVersion === ElasticSearchSupportedVersions.V8) {
			client = es8({ nodes: esHost, version: ElasticSearchSupportedVersions.V8, basicAuth: { enabled: false } });
		}

		await new Promise((resolve) => setTimeout(resolve, 5000));
	});

	after(async () => {
		await container.stop();
	});

	it('reports a document missing from the index as not found', async () => {
		const indexName = 'cleanup-delete-missing';
		await client.createIndex(indexName);

		const result = await client.deleteData(indexName, 'absent');

		expect(result).to.eql({ failureData: { absent: [DOCUMENT_NOT_FOUND] }, indexName, successful: false });
	});

	it('logs an error naming the index and the document when the index holds no such document', async () => {
		const indexName = 'cleanup-delete-missing-log';
		await client.createIndex(indexName);
		const logs = recordLogs();

		try {
			await client.deleteData(indexName, 'absent');
		} finally {
			logs.restore();
		}

		expect(logs.entries.filter((entry) => entry.level === 'error').map((entry) => entry.args)).to.eql([
			[{ documentId: 'absent', event: 'maestro.indexer.deleteNotFound', indexName, status: 404 }],
		]);
	});

	it('deletes a document held by the index, so deleting it again reports it not found', async () => {
		const indexName = 'cleanup-delete-present';
		await client.bulkUpsert(indexName, [{ _id: 'present', organization: 'org-a' }]);

		const result = await client.deleteData(indexName, 'present');
		const repeated = await client.deleteData(indexName, 'present');

		expect(result.successful).to.equal(true);
		expect(repeated.failureData).to.eql({ present: [DOCUMENT_NOT_FOUND] });
	});

	it('lists every document with its field value, across pages', async () => {
		const indexName = 'cleanup-scan-all';
		await client.bulkUpsert(indexName, [
			{ _id: 'rec-1', organization: 'org-a' },
			{ _id: 'rec-2', organization: 'org-b' },
			{ _id: 'rec-3', organization: 'org-a' },
		]);

		const documents = await scan(indexName, { fieldName: 'organization', pageSize: 2 });

		expect(documents.toSorted(byDocumentId)).to.eql([
			{ documentId: 'rec-1', fieldValue: 'org-a' },
			{ documentId: 'rec-2', fieldValue: 'org-b' },
			{ documentId: 'rec-3', fieldValue: 'org-a' },
		]);
	});

	it('lists only the documents whose field value matches exactly', async () => {
		const indexName = 'cleanup-scan-exact';
		await createIndexMappingOrganizationAsKeyword(indexName);
		await client.bulkUpsert(indexName, [
			{ _id: 'exact', organization: 'abc' },
			{ _id: 'upper', organization: 'ABC' },
			{ _id: 'words', organization: 'abc lab' },
			{ _id: 'hyphen', organization: 'abc-123' },
		]);

		const documents = await scan(indexName, { fieldName: 'organization', fieldValue: 'abc' });

		expect(documents).to.eql([{ documentId: 'exact', fieldValue: 'abc' }]);
	});

	it('lists the documents holding exactly a value written in capitals', async () => {
		const indexName = 'cleanup-scan-capitals';
		await createIndexMappingOrganizationAsKeyword(indexName);
		await client.bulkUpsert(indexName, [
			{ _id: 'exact', organization: 'ON' },
			{ _id: 'lower', organization: 'on' },
			{ _id: 'words', organization: 'ON lab' },
		]);

		const documents = await scan(indexName, { fieldName: 'organization', fieldValue: 'ON' });

		expect(documents).to.eql([{ documentId: 'exact', fieldValue: 'ON' }]);
	});

	it('releases its place in the index when the reader stops after the first page', async () => {
		const indexName = 'cleanup-scan-stopped';
		await client.bulkUpsert(indexName, [
			{ _id: 'rec-1', organization: 'org-a' },
			{ _id: 'rec-2', organization: 'org-a' },
			{ _id: 'rec-3', organization: 'org-a' },
		]);

		for await (const page of listDocuments(indexName, { fieldName: 'organization', pageSize: 1 })) {
			expect(page).to.have.length(1);
			break;
		}

		expect(await countOpenScrolls()).to.equal(0);
	});

	it('lists nothing for an index holding no documents', async () => {
		const indexName = 'cleanup-scan-empty';
		await client.createIndex(indexName);

		expect(await scan(indexName, { fieldName: 'organization' })).to.eql([]);
	});
}
