import {
	type ElasticsearchService,
	type KafkaConfig,
	logger,
	type LyricRepositoryConfig,
	type RepositoryIndexingOperations,
	type SongRepositoryConfig,
} from '@overture-stack/maestro-common';

import { client } from './client.js';
import { processRequestMessage } from './processMessage/requestMessage.js';
import { routeDocumentMessage } from './processMessage/routeDocumentMessage.js';
import { sendToDLQ } from './producer.js';
import { getRepoTopics } from './repositoryUtils.js';

/**
 * Initialize a Kafka consumer to listen to each repository and request topics
 * @param kafkaConfig - Kafka configuration
 * @param repositories - A list of repository configurations (e.g. Song or Lyric)
 * @param indexerProvider - An instance of `ElasticsearchService` used for indexing or deleting documents in Elasticsearch.
 * @param repositoryIndexingApi - API that provides repository-specific fetching and indexing logic.
 * @returns
 */
export async function initializeConsumer({
	kafkaConfig,
	repositories,
	indexerProvider,
	repositoryIndexingApi,
}: {
	kafkaConfig: KafkaConfig;
	repositories: (SongRepositoryConfig | LyricRepositoryConfig)[];
	indexerProvider: ElasticsearchService;
	repositoryIndexingApi: RepositoryIndexingOperations;
}) {
	if (!kafkaConfig.brokers || !kafkaConfig.groupId) {
		logger.info('Kafka brokers is not configured, skipping consumer initialization');
		return;
	}

	const groupId = kafkaConfig.groupId;
	const kafka = await client(kafkaConfig);
	const consumer = kafka.consumer({ groupId });
	const producer = kafka.producer({ allowAutoTopicCreation: true });

	await consumer.connect();
	await producer.connect();

	const repositoryTopics = getRepoTopics(repositories);

	const requestTopic = kafkaConfig.requestBinding?.topic;

	const topics = [...repositoryTopics];
	if (requestTopic) {
		topics.push(requestTopic);
	}

	if (!topics.length) {
		logger.error('No topics found in configuration');
		return;
	}

	const admin = kafka.admin();
	try {
		await admin.connect();
		await admin.createTopics({
			waitForLeaders: true,
			topics: topics.map((t) => ({
				topic: t,
			})),
		});
	} catch (error) {
		logger.warn('Topic pre-creation failed; topics may already exist or require manual creation', { error });
	} finally {
		await admin.disconnect();
	}

	await consumer.subscribe({ topics, fromBeginning: true });
	logger.info(`Subscribing to Kafka topics: ${JSON.stringify(topics)}`);

	await consumer.run({
		eachMessage: async ({ topic, message }) => {
			logger.info(`Processing a message on topic '${topic}'`);
			if (!message || !message.value) {
				logger.info(`[${topic}]: Received empty or null message`);
				return;
			}

			if (requestTopic && topic === requestTopic) {
				try {
					await processRequestMessage({
						repositories,
						message: message,
						indexer: indexerProvider,
						repositoryIndexingApi: repositoryIndexingApi,
					});
				} catch (error) {
					logger.error(`Failed to process request message. ${error}`);
					await sendToDLQ(producer, message, kafkaConfig.requestBinding?.dlq);
				}
				return;
			}

			await routeDocumentMessage({
				indexer: indexerProvider,
				message,
				producer,
				repositories,
				topic,
			});
			return;
		},
	});
}
