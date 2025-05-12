import {
	type ElasticsearchService,
	type KafkaConfig,
	logger,
	type LyricRepositoryConfig,
	type RepositoryIndexingOperations,
	type SongRepositoryConfig,
} from '@overture-stack/maestro-common';

import { client } from './client.js';
import { processDocumentMessage } from './processMessage/documentMessage.js';
import { processRequestMessage } from './processMessage/requestMessage.js';
import { sendToDLQ } from './producer.js';
import { getRepoByTopic, getRepoTopics } from './repositoryUtils.js';

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
	if (kafkaConfig.server) {
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

		await consumer.subscribe({ topics, fromBeginning: true });
		logger.info(`Subscribing to Kafka topics: ${JSON.stringify(topics)}`);

		await consumer.run({
			eachMessage: async ({ topic, message }) => {
				if (!message || !message.value) {
					logger.info(`[${topic}]: Received empty or null message`);
					return;
				}

				const repo = getRepoByTopic(repositories, topic);
				if (!repo) {
					await sendToDLQ(producer, message);
					return;
				}

				if (requestTopic && topic === requestTopic) {
					await processRequestMessage({
						repository: repo,
						message: message,
						indexer: indexerProvider,
						repositoryIndexingApi: repositoryIndexingApi,
					});
					return;
				}

				try {
					await processDocumentMessage({ repository: repo, message: message, indexer: indexerProvider });
					return;
				} catch (error) {
					logger.error('Failed to send message to dead letter queue', { error });
					await sendToDLQ(producer, message, repo.kafkaDlq);
				}
			},
		});
	}
}
