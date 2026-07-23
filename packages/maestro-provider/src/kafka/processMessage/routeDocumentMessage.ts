import type { KafkaMessage, Producer } from 'kafkajs';

import type { ElasticsearchService, LyricRepositoryConfig, SongRepositoryConfig } from '@overture-stack/maestro-common';
import { logger } from '@overture-stack/maestro-common';

import { sendToDLQ } from '../producer.js';
import { getConfiguredCategoryIds, getMatchingRepos } from '../repositoryUtils.js';
import { parseMessage } from './parser.js';
import { processDocumentMessage } from './documentMessage.js';

/**
 * Routes a message received from Kafka to the correct processor.
 * 
 * Parses a Lyric/SONG document message once, resolves which repo(s) it belongs to, and indexes
 * it into every match. Split out from `initializeConsumer`'s `eachMessage` so it's testable
 * without a live Kafka client, and so a fan-out doesn't re-parse the same message per repo.
 * @param indexer - The Elasticsearch service used to interact with the index
 * @param message - The raw Kafka message
 * @param producer - The Kafka producer used to send unroutable/failed messages to a DLQ
 * @param repositories - Every configured repository (SONG and Lyric), not just Lyric ones
 * @param topic - The Kafka topic the message arrived on
 */
export const routeDocumentMessage = async ({
	indexer,
	message,
	producer,
	repositories,
	topic,
}: {
	indexer: ElasticsearchService;
	message: KafkaMessage;
	producer: Producer;
	repositories: (SongRepositoryConfig | LyricRepositoryConfig)[];
	topic: string;
}): Promise<void> => {
	const parsed = parseMessage(message.value);
	if (!parsed) {
		logger.warn(`Invalid message format on topic '${topic}', sending to DLQ`, { topic });
		await sendToDLQ(producer, message);
		return;
	}

	const matches = getMatchingRepos(repositories, topic, {
		categoryAlias: typeof parsed.categoryAlias === 'string' ? parsed.categoryAlias : undefined,
		categoryId:
			typeof parsed.categoryId === 'string' || typeof parsed.categoryId === 'number' ? parsed.categoryId : undefined,
	});

	if (matches.length === 0) {
		// Not DLQ'd: a well-formed message with no matching repo isn't an error to reprocess, it's
		// expected whenever a topic carries categories this Maestro deployment isn't configured to
		// index (Lyric's publishing isn't under Maestro's control). configuredForTopic is logged
		// alongside it so a genuine misconfiguration (e.g. a typo'd categoryId) is still visible to
		// anyone looking, without treating every non-match as an operational alarm.
		logger.info(`No repository matched message on topic '${topic}', skipping`, {
			categoryAlias: parsed.categoryAlias,
			categoryId: parsed.categoryId,
			configuredForTopic: getConfiguredCategoryIds(repositories, topic),
			topic,
		});
		return;
	}

	logger.info(`Routing message on topic '${topic}' to ${matches.length} repo(s)`, {
		categoryAlias: parsed.categoryAlias,
		categoryId: parsed.categoryId,
		matchedRepos: matches.map((repository) => repository.code),
		topic,
	});

	await Promise.all(
		matches.map(async (repository) => {
			try {
				// A fresh shallow copy per repository: downstream handlers mutate their payload directly,
				// and fan-out means more than one can now receive the same message concurrently.
				await processDocumentMessage({ indexer, payload: { ...parsed }, repository });
			} catch (error) {
				logger.error(`Failed to process message for repository '${repository.code}'`, { error });
				await sendToDLQ(producer, message, repository.kafkaDlq);
			}
		}),
	);
};
