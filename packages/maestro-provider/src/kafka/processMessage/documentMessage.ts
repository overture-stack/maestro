import type { KafkaMessage } from 'kafkajs';

import type { ElasticsearchService, LyricRepositoryConfig, SongRepositoryConfig } from '@overture-stack/maestro-common';
import { logger, RepositoryType } from '@overture-stack/maestro-common';

import { handleLyricDocumentMessage } from './lyric/handleDocument.js';
import { parseMessage } from './parser.js';
import { handleSongDocumentMessage } from './song/handleDocument.js';

/**
 * Processes a Document Message from Kafka and performs the appropriate action based on the repository type (SONG or Lyric)
 * @param repository - The repository configuration, which can either be a `SongRepositoryConfig` or a `LyricRepositoryConfig`
 * @param message - The Kafka message containing the document data
 * @param indexer - The Elasticsearch service used to interact with the index
 */
export const processDocumentMessage = async ({
	repository,
	message,
	indexer,
}: {
	repository: SongRepositoryConfig | LyricRepositoryConfig;
	message: KafkaMessage;
	indexer: ElasticsearchService;
}) => {
	const parsed = parseMessage(message.value);
	if (!parsed) {
		throw new Error('Invalid message format');
	}

	// Depending on the repository type (SONG or LYRIC), the function delegates to the appropriate handler
	if (repository.type === RepositoryType.SONG) {
		logger.info(`Processing SONG kafka message for document in repository '${repository.code}'`);
		await handleSongDocumentMessage(repository, parsed, indexer);
	} else if (repository.type === RepositoryType.LYRIC) {
		logger.info(`Processing Lyric kafka message for document inrepository '${repository.code}'`);
		await handleLyricDocumentMessage(repository, parsed, indexer);
	}
};
