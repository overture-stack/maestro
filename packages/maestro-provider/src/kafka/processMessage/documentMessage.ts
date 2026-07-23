import type {
	DataRecordNested,
	ElasticsearchService,
	LyricRepositoryConfig,
	SongRepositoryConfig,
} from '@overture-stack/maestro-common';
import { logger, RepositoryType } from '@overture-stack/maestro-common';

import { handleLyricDocumentMessage } from './lyric/handleDocument.js';
import { handleSongDocumentMessage } from './song/handleDocument.js';

/**
 * Processes an already-parsed Document Message and performs the appropriate action based on the
 * repository type (SONG or Lyric). Takes the parsed `payload`, not the raw Kafka message, so a
 * fan-out caller doesn't re-parse the same message once per repository.
 * @param repository - The repository configuration, which can either be a `SongRepositoryConfig` or a `LyricRepositoryConfig`
 * @param payload - The already-parsed document data
 * @param indexer - The Elasticsearch service used to interact with the index
 */
export const processDocumentMessage = async ({
	indexer,
	payload,
	repository,
}: {
	indexer: ElasticsearchService;
	payload: DataRecordNested;
	repository: SongRepositoryConfig | LyricRepositoryConfig;
}) => {
	// Depending on the repository type (SONG or LYRIC), the function delegates to the appropriate handler
	if (repository.type === RepositoryType.SONG) {
		logger.info(`Processing SONG kafka message for document in repository '${repository.code}'`);
		await handleSongDocumentMessage(repository, payload, indexer);
	} else if (repository.type === RepositoryType.LYRIC) {
		logger.info(`Processing Lyric kafka message for document inrepository '${repository.code}'`);
		await handleLyricDocumentMessage(repository, payload, indexer);
	}
};
