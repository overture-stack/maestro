import type { KafkaMessage } from 'kafkajs';

import {
	type ElasticsearchService,
	type LyricRepositoryConfig,
	type RepositoryIndexingOperations,
	RepositoryType,
	type SongRepositoryConfig,
} from '@overture-stack/maestro-common';

import { handleLyricRequestMessage } from './lyric/handleRequest.js';
import { parseMessage } from './parser.js';
import { handleSongRequestMessage } from './song/handleRequest.js';

/**
 * Parses the kafka Request message and sends it to Song or Lyric handlers
 * @param param0
 * @returns
 */
export const processRequestMessage = async ({
	repository,
	message,
	indexer,
	repositoryIndexingApi,
}: {
	repository: SongRepositoryConfig | LyricRepositoryConfig;
	message: KafkaMessage;
	indexer: ElasticsearchService;
	repositoryIndexingApi: RepositoryIndexingOperations;
}) => {
	const parsed = parseMessage(message.value);
	if (!parsed) {
		throw new Error('Invalid message format');
	}
	const repositoryCode = parsed.repositoryCode;
	if (repositoryCode !== repository.code) {
		return;
	}

	if (repository.type === RepositoryType.SONG) {
		await handleSongRequestMessage(repository, parsed, indexer, repositoryIndexingApi);
	} else if (repository.type === RepositoryType.LYRIC) {
		await handleLyricRequestMessage(repository, parsed, indexer, repositoryIndexingApi);
	}
};
