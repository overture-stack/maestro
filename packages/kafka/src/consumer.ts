import {
	type DataRecordValue,
	type IElasticsearchService,
	type KafkaConfig,
	logger,
	type LyricRepositoryConfig,
	RepositoryType,
	type SongRepositoryConfig,
} from '@overture-stack/maestro-common';

import { client } from './client.js';

const groupId = 'groupy';

const isDefined = (item: string | undefined): item is string => {
	return !!item;
};

const getRepoByTopic = (repos: (SongRepositoryConfig | LyricRepositoryConfig)[], topic: string) => {
	return repos.find((repo) => repo.kafkaTopic === topic);
};

const parseMessage = (messageValue: Buffer | null): Record<string, DataRecordValue>[] | null => {
	if (!messageValue) return null;
	try {
		return JSON.parse(messageValue.toString());
	} catch {
		console.error('Invalid JSON message:', messageValue?.toString());
		return null;
	}
};

const handleSongMessage = async (
	indexName: string,
	payload: Record<string, DataRecordValue>[],
	indexer: IElasticsearchService,
) => {
	const data = Array.isArray(payload) ? payload : [payload];
	await indexer.bulkUpsert(indexName, data);
};

const handleLyricMessage = async (
	indexName: string,
	payload: Record<string, DataRecordValue>[],
	indexer: IElasticsearchService,
) => {
	const data = Array.isArray(payload) ? payload : [payload];
	await indexer.bulkUpsert(indexName, data);
};

export async function initializeConsumer({
	kafkaConfig,
	repositories,
	indexerProvider,
}: {
	kafkaConfig: KafkaConfig;
	repositories: (SongRepositoryConfig | LyricRepositoryConfig)[];
	indexerProvider: IElasticsearchService;
}) {
	if (kafkaConfig.servers) {
		const kafka = client(kafkaConfig);
		const consumer = kafka.consumer({ groupId: groupId });
		await consumer.connect();

		const repoTopics = repositories.map((repo) => repo.kafkaTopic).filter(isDefined);
		const topics = kafkaConfig.requestBinding?.topic ? [...repoTopics, kafkaConfig.requestBinding.topic] : repoTopics;

		await consumer.subscribe({ topics, fromBeginning: true });

		await consumer.run({
			eachMessage: async ({ topic, message }) => {
				const repo = getRepoByTopic(repositories, topic);
				if (!repo || !message) return;

				const parsed = parseMessage(message.value);
				if (!parsed) return;
				if (repo.type === RepositoryType.SONG) {
					await handleSongMessage(repo.indexName, parsed, indexerProvider);
				} else if (repo.type === RepositoryType.LYRIC) {
					await handleLyricMessage(repo.indexName, parsed, indexerProvider);
				}
				logger.info(`${groupId}: [${topic}]:`, message.value?.toString());
			},
		});
		return consumer;
	}
}
