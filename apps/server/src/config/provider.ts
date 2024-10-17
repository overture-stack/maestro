import type { Config } from '@overture-stack/maestro-common';

import { env } from './envConfig.js';

export const defaultAppConfig: Config = {
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
		level: env.MAESTRO_LOGGING_LEVEL_ROOT,
	},
	lyricConfig: {
		index: {
			alias: env.MAESTRO_LYRIC_INDEX_ALIAS,
			enabled: env.MAESTRO_LYRIC_INDEX_ENABLED,
			name: env.MAESTRO_LYRIC_INDEX_NAME,
		},
		indexValidDataOnly: env.MAESTRO_LYRIC_INDEX_VALID_DATA_ONLY,
		repositories: env.MAESTRO_LYRIC_REPOSITORIES,
	},
	songConfig: {
		indexableStudyStates: env.MAESTRO_SONG_INDEXABLE_STUDY_STATES,
		indices: {
			analysisCentric: {
				alias: env.MAESTRO_SONG_INDEX_ANALYSISCENTRIC_ALIAS,
				enabled: env.MAESTRO_SONG_INDEX_ANALYSISCENTRIC_ENABLED,
				name: env.MAESTRO_SONG_INDEX_ANALYSISCENTRIC_NAME,
			},
			fileCentric: {
				alias: env.MAESTRO_SONG_INDEX_FILECENTRIC_ALIAS,
				enabled: env.MAESTRO_SONG_INDEX_FILECENTRIC_ENABLED,
				name: env.MAESTRO_SONG_INDEX_FILECENTRIC_NAME,
			},
		},
		repositories: env.MAESTRO_SONG_REPOSITORIES,
	},
};
