import { z } from 'zod';

import type {
	LyricRepositoryConfig,
	MaestroProviderConfig,
	SongRepositoryConfig,
} from '@overture-stack/maestro-common';

import { logger, setLogLevel } from '../utils/logger.js';
import { env } from './envConfig.js';
import { type lyricSchemaDefinition, repositoryTypes, type songSchemaDefinition } from './repositoryConfig.js';

setLogLevel(env.MAESTRO_LOGGING_LEVEL);

const getRepositoryConfig = (
	repos: (z.infer<typeof lyricSchemaDefinition> | z.infer<typeof songSchemaDefinition>)[],
): (LyricRepositoryConfig | SongRepositoryConfig)[] => {
	const lyricRepos: LyricRepositoryConfig[] = repos
		.filter((value) => value && value.TYPE === repositoryTypes.Values.LYRIC)
		.map((value) => ({
			baseUrl: value.BASE_URL,
			code: value.CODE,
			name: value.NAME,
			paginationSize: value.PAGINATION_SIZE,
			type: repositoryTypes.Values.LYRIC,
			indexName: value.INDEX_NAME,
			validDataOnly: value.LYRIC_VALID_DATA_ONLY,
			categoryId: value.LYRIC_CATEGORY_ID,
		}));

	const songRepos: SongRepositoryConfig[] = repos
		.filter((value) => value && value.TYPE === repositoryTypes.Values.SONG)
		.map((value) => ({
			baseUrl: value.BASE_URL,
			code: value.CODE,
			name: value.NAME,
			paginationSize: value.PAGINATION_SIZE,
			type: repositoryTypes.Values.SONG,
			indexName: value.INDEX_NAME,
			indexableStudyStates: value.SONG_INDEXABLE_STUDY_STATES,
			analysisCentricEnabled: value.SONG_ANALYSIS_CENTRIC_ENABLED,
			organization: value.SONG_ORGANIZATION,
			country: value.SONG_COUNTRY,
		}));
	return [...songRepos, ...lyricRepos];
};

export const defaultAppConfig: MaestroProviderConfig = {
	elasticSearchConfig: {
		basicAuth: {
			enabled: env.MAESTRO_ELASTICSEARCH_CLIENT_BASICAUTH_ENABLED,
			user: env.MAESTRO_ELASTICSEARCH_CLIENT_BASICAUTH_USER,
			password: env.MAESTRO_ELASTICSEARCH_CLIENT_BASICAUTH_PASSWORD,
		},
		nodes: env.MAESTRO_ELASTICSEARCH_NODES,
		version: env.MAESTRO_ELASTICSEARCH_VERSION,
		connectionTimeOut: env.MAESTRO_ELASTICSEARCH_CLIENT_CONNECTION_TIMEOUT,
		docsPerBulkReqMax: env.MAESTRO_ELASTICSEARCH_CLIENT_DOCS_PER_BULK_REQ_MAX,
		retry: {
			retryMaxAttempts: env.MAESTRO_ELASTICSEARCH_CLIENT_RETRY_MAX_ATTEMPTS,
			retryWaitDurationMillis: env.MAESTRO_ELASTICSEARCH_CLIENT_RETRY_WAIT_DURATION_MILLIS,
		},
	},
	kafka: {
		enabled: env.MAESTRO_KAFKA_ENABLED,
		servers: env.MAESTRO_KAFKA_SERVERS,
		lyricSchemaBinding: {
			analysisMessage: {
				dlq: env.MAESTRO_KAFKA_LYRIC_ANALYSIS_MESSAGE_DLQ,
				topic: env.MAESTRO_KAFKA_LYRIC_ANALYSIS_MESSAGE_TOPIC,
			},
			requestMessage: {
				dlq: env.MAESTRO_KAFKA_LYRIC_REQUEST_MESSAGE_DLQ,
				topic: env.MAESTRO_KAFKA_LYRIC_REQUEST_MESSAGE_TOPIC,
			},
		},
		songSchemaBinding: {
			analysisMessage: {
				dlq: env.MAESTRO_KAFKA_SONG_ANALYSIS_MESSAGE_DLQ,
				topic: env.MAESTRO_KAFKA_SONG_ANALYSIS_MESSAGE_TOPIC,
			},
			requestMessage: {
				dlq: env.MAESTRO_KAFKA_SONG_REQUEST_MESSAGE_DLQ,
				topic: env.MAESTRO_KAFKA_SONG_REQUEST_MESSAGE_TOPIC,
			},
		},
	},
	logger: {
		logger: logger,
	},
	repositories: getRepositoryConfig(env.repositories),
};
