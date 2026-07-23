import { expect } from 'chai';
import { afterEach, beforeEach, describe, it } from 'mocha';
import sinon from 'sinon';

import {
	type ElasticsearchService,
	IndexableState,
	type LyricRepositoryConfig,
	logger,
	type SongRepositoryConfig,
} from '@overture-stack/maestro-common';

import { routeDocumentMessage } from '../../src/kafka/processMessage/routeDocumentMessage';

describe('routeDocumentMessage', () => {
	const sharedTopic = 'lyric-document-updates';

	const lyricRepo = (categoryId: string, indexName = `index-${categoryId}`): LyricRepositoryConfig => ({
		baseUrl: 'lyric-server',
		categoryId,
		code: `code-${categoryId}-${indexName}`,
		indexName,
		kafkaTopic: sharedTopic,
		name: `Lyric ${categoryId}`,
		type: 'LYRIC',
		validDataOnly: false,
	});

	const kafkaMessage = (value: unknown) => ({
		value: Buffer.from(JSON.stringify(value)),
	});

	const createFakeIndexer = () => ({
		bulkUpsert: sinon.stub().resolves({ status: 'success' }),
		deleteData: sinon.stub().resolves({ status: 'success' }),
	});

	const createFakeProducer = () => ({
		send: sinon.stub().resolves(),
	});

	let loggerWarnStub: sinon.SinonStub;
	let loggerErrorStub: sinon.SinonStub;
	let loggerInfoStub: sinon.SinonStub;

	beforeEach(() => {
		loggerWarnStub = sinon.stub(logger, 'warn');
		loggerErrorStub = sinon.stub(logger, 'error');
		loggerInfoStub = sinon.stub(logger, 'info');
	});

	afterEach(() => {
		sinon.restore();
	});

	describe('when the topic maps to a single Lyric repo', () => {
		it('should skip (not DLQ) a message with no categoryId, since a lone repo still requires a match', async () => {
			const repo = lyricRepo('3');
			const indexer = createFakeIndexer();
			const producer = createFakeProducer();

			await routeDocumentMessage({
				indexer: indexer as unknown as ElasticsearchService,
				message: kafkaMessage({ data: { a: 1 }, isValid: true, systemId: 'SYS-1' }) as never,
				producer: producer as never,
				repositories: [repo],
				topic: sharedTopic,
			});

			expect(indexer.bulkUpsert.called).to.be.false;
			expect(producer.send.called).to.be.false;
			expect(loggerInfoStub.called).to.be.true;
		});

		it('should index the document when the message categoryId matches the configured value', async () => {
			const repo = lyricRepo('3');
			const indexer = createFakeIndexer();
			const producer = createFakeProducer();

			await routeDocumentMessage({
				indexer: indexer as unknown as ElasticsearchService,
				message: kafkaMessage({ categoryId: '3', data: { a: 1 }, isValid: true, systemId: 'SYS-1' }) as never,
				producer: producer as never,
				repositories: [repo],
				topic: sharedTopic,
			});

			expect(indexer.bulkUpsert.called).to.be.true;
			expect(producer.send.called).to.be.false;
		});

		it('should send to the repo-specific DLQ and log an error when indexing fails', async () => {
			const repo = { ...lyricRepo('3'), kafkaDlq: 'lyric-3-dlq' };
			const producer = createFakeProducer();
			const indexer = {
				bulkUpsert: sinon.stub().rejects(new Error('elasticsearch unavailable')),
				deleteData: sinon.stub().resolves({ status: 'success' }),
			};

			await routeDocumentMessage({
				indexer: indexer as unknown as ElasticsearchService,
				message: kafkaMessage({ categoryId: '3', data: { a: 1 }, isValid: true, systemId: 'SYS-1' }) as never,
				producer: producer as never,
				repositories: [repo],
				topic: sharedTopic,
			});

			expect(producer.send.calledWith(sinon.match({ topic: 'lyric-3-dlq' }))).to.be.true;
			expect(loggerErrorStub.called).to.be.true;
		});
	});

	describe('when the topic maps to more than one repo', () => {
		it('should index into the repo matching the categoryAlias', async () => {
			const donorRepo = lyricRepo('donor');
			const otherRepo = lyricRepo('mutation');
			const indexer = createFakeIndexer();
			const producer = createFakeProducer();

			await routeDocumentMessage({
				indexer: indexer as unknown as ElasticsearchService,
				message: kafkaMessage({ categoryAlias: 'donor', categoryId: 9, isValid: true, systemId: 'SYS-1' }) as never,
				producer: producer as never,
				repositories: [donorRepo, otherRepo],
				topic: sharedTopic,
			});

			expect(indexer.bulkUpsert.calledWith(donorRepo.indexName)).to.be.true;
			expect(indexer.bulkUpsert.calledWith(otherRepo.indexName)).to.be.false;
		});

		it('should fall back to the categoryId when the message has no categoryAlias', async () => {
			const repoA = lyricRepo('3');
			const repoB = lyricRepo('7');
			const indexer = createFakeIndexer();
			const producer = createFakeProducer();

			await routeDocumentMessage({
				indexer: indexer as unknown as ElasticsearchService,
				message: kafkaMessage({ categoryId: 7, isValid: true, systemId: 'SYS-1' }) as never,
				producer: producer as never,
				repositories: [repoA, repoB],
				topic: sharedTopic,
			});

			expect(indexer.bulkUpsert.calledWith(repoB.indexName)).to.be.true;
			expect(indexer.bulkUpsert.calledWith(repoA.indexName)).to.be.false;
		});

		it('should index into every repo sharing the same configured identifier (fan-out)', async () => {
			const repoA = lyricRepo('donor', 'donor-mapping-a');
			const repoB = lyricRepo('donor', 'donor-mapping-b');
			const indexer = createFakeIndexer();
			const producer = createFakeProducer();

			await routeDocumentMessage({
				indexer: indexer as unknown as ElasticsearchService,
				message: kafkaMessage({ categoryAlias: 'donor', categoryId: 9, isValid: true, systemId: 'SYS-1' }) as never,
				producer: producer as never,
				repositories: [repoA, repoB],
				topic: sharedTopic,
			});

			expect(indexer.bulkUpsert.calledWith(repoA.indexName)).to.be.true;
			expect(indexer.bulkUpsert.calledWith(repoB.indexName)).to.be.true;
		});

		it('should skip (not DLQ) and log info when no repo matches the message categoryId/categoryAlias', async () => {
			// Not an error: a well-formed message that simply isn't for any configured repo has
			// nothing to reprocess, expected once a topic carries categories this Maestro isn't
			// configured to index.
			const repoA = lyricRepo('3');
			const repoB = lyricRepo('7');
			const indexer = createFakeIndexer();
			const producer = createFakeProducer();

			await routeDocumentMessage({
				indexer: indexer as unknown as ElasticsearchService,
				message: kafkaMessage({ categoryAlias: 'unknown', categoryId: 99, isValid: true, systemId: 'SYS-1' }) as never,
				producer: producer as never,
				repositories: [repoA, repoB],
				topic: sharedTopic,
			});

			expect(indexer.bulkUpsert.called).to.be.false;
			expect(producer.send.called).to.be.false;
			expect(loggerInfoStub.called).to.be.true;
		});

		it('should include every repo configured for the topic in the no-match log, so a misconfigured categoryId is visible without cross-referencing deployed config', async () => {
			const repoA = lyricRepo('3');
			const repoB = lyricRepo('7');
			const indexer = createFakeIndexer();
			const producer = createFakeProducer();

			await routeDocumentMessage({
				indexer: indexer as unknown as ElasticsearchService,
				message: kafkaMessage({ categoryId: 'blah', isValid: true, systemId: 'SYS-1' }) as never,
				producer: producer as never,
				repositories: [repoA, repoB],
				topic: sharedTopic,
			});

			const loggedContext = loggerInfoStub.getCalls().flatMap((call) => call.args);
			const serialized = JSON.stringify(loggedContext);
			expect(serialized).to.include('blah');
			expect(serialized).to.include('"configuredForTopic":["3","7"]');
		});

		it('should skip (not DLQ) and log info when the message carries no category identifier at all', async () => {
			const repoA = lyricRepo('3');
			const repoB = lyricRepo('7');
			const indexer = createFakeIndexer();
			const producer = createFakeProducer();

			await routeDocumentMessage({
				indexer: indexer as unknown as ElasticsearchService,
				message: kafkaMessage({ isValid: true, systemId: 'SYS-1' }) as never,
				producer: producer as never,
				repositories: [repoA, repoB],
				topic: sharedTopic,
			});

			expect(indexer.bulkUpsert.called).to.be.false;
			expect(producer.send.called).to.be.false;
			expect(loggerInfoStub.called).to.be.true;
		});
	});

	describe('per-repo failures during fan-out', () => {
		it('should send only the failing repo to its own DLQ while the other still gets indexed', async () => {
			const repoA = lyricRepo('donor', 'donor-mapping-a');
			const repoB = { ...lyricRepo('donor', 'donor-mapping-b'), kafkaDlq: 'donor-mapping-b-dlq' };
			const producer = createFakeProducer();

			const indexer = {
				bulkUpsert: sinon.stub().callsFake(async (index: string) => {
					if (index === repoA.indexName) {
						throw new Error('elasticsearch unavailable');
					}
					return { status: 'success' };
				}),
				deleteData: sinon.stub().resolves({ status: 'success' }),
			};

			await routeDocumentMessage({
				indexer: indexer as unknown as ElasticsearchService,
				message: kafkaMessage({ categoryAlias: 'donor', categoryId: 9, isValid: true, systemId: 'SYS-1' }) as never,
				producer: producer as never,
				repositories: [repoA, repoB],
				topic: sharedTopic,
			});

			expect(indexer.bulkUpsert.callCount).to.equal(2);
			// repoA failed and has no custom kafkaDlq, so its failure falls back to the default DLQ topic
			expect(producer.send.calledWith(sinon.match({ topic: 'default-dlq' }))).to.be.true;
			// repoB's own DLQ is never touched, since repoB succeeded
			expect(producer.send.calledWith(sinon.match({ topic: repoB.kafkaDlq }))).to.be.false;
			expect(loggerErrorStub.called).to.be.true;
		});
	});

	describe('logging', () => {
		it('should log the categoryId, categoryAlias, and matched repo codes on every successful routing decision', async () => {
			const repo = lyricRepo('donor');
			const indexer = createFakeIndexer();
			const producer = createFakeProducer();

			await routeDocumentMessage({
				indexer: indexer as unknown as ElasticsearchService,
				message: kafkaMessage({ categoryAlias: 'donor', categoryId: 9, isValid: true, systemId: 'SYS-1' }) as never,
				producer: producer as never,
				repositories: [repo],
				topic: sharedTopic,
			});

			const loggedContext = loggerInfoStub.getCalls().flatMap((call) => call.args);
			const serialized = JSON.stringify(loggedContext);
			expect(serialized).to.include('donor');
			expect(serialized).to.include('9');
		});
	});

	describe('malformed messages', () => {
		it('should send to the DLQ and log a warning when the message is not valid JSON', async () => {
			const repo = lyricRepo('3');
			const indexer = createFakeIndexer();
			const producer = createFakeProducer();

			await routeDocumentMessage({
				indexer: indexer as unknown as ElasticsearchService,
				message: { value: Buffer.from('not json') } as never,
				producer: producer as never,
				repositories: [repo],
				topic: sharedTopic,
			});

			expect(indexer.bulkUpsert.called).to.be.false;
			expect(producer.send.called).to.be.true;
			expect(loggerWarnStub.called).to.be.true;
		});
	});

	describe('SONG repositories', () => {
		it('should continue to route SONG messages by topic alone, unaffected by categoryId/categoryAlias matching', async () => {
			const songRepo: SongRepositoryConfig = {
				baseUrl: 'song-server',
				code: 'SONG1',
				// Non-indexable for this state, so the handler takes the delete path (only needs analysisId).
				indexableStudyStates: [IndexableState.PUBLISHED],
				indexingMode: 'analysis' as SongRepositoryConfig['indexingMode'],
				indexName: 'analysis_centric',
				kafkaTopic: 'song-analysis',
				name: 'Song 1',
				type: 'SONG',
			};
			const indexer = createFakeIndexer();
			const producer = createFakeProducer();

			await routeDocumentMessage({
				indexer: indexer as unknown as ElasticsearchService,
				message: kafkaMessage({ analysisId: 'a-1', state: 'UNPUBLISHED', studyId: 's-1' }) as never,
				producer: producer as never,
				repositories: [songRepo],
				topic: 'song-analysis',
			});

			expect(producer.send.called).to.be.false;
		});
	});
});
