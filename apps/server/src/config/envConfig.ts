import 'dotenv/config';

import { z } from 'zod';

export const getServerConfig = () => {
	return {
		port: process.env.MAESTRO_SERVER_PORT || 11235,
		nodeEnv: process.env.NODE_ENV || 'development',
		openApiPath: process.env.MAESTRO_OPENAPI_PATH || 'api-docs',
	};
};

const elasticSearchConfigSchema = z.object({
	MAESTRO_ELASTICSEARCH_CLIENT_BASICAUTH_ENABLED: z.coerce.boolean(),
	MAESTRO_ELASTICSEARCH_CLIENT_BASICAUTH_PASSWORD: z.string().optional(),
	MAESTRO_ELASTICSEARCH_CLIENT_BASICAUTH_USER: z.string().optional(),
	MAESTRO_ELASTICSEARCH_CLIENT_CONNECTION_TIMEOUT: z.coerce.number().optional(),
	MAESTRO_ELASTICSEARCH_CLIENT_DOCS_PER_BULK_REQ_MAX: z.coerce.number().default(5000),
	MAESTRO_ELASTICSEARCH_CLIENT_RETRY_MAX_ATTEMPTS: z.coerce.number().optional(),
	MAESTRO_ELASTICSEARCH_CLIENT_RETRY_WAIT_DURATION_MILLIS: z.coerce.number().optional(),
	MAESTRO_ELASTICSEARCH_NODES: z.string(),
	MAESTRO_ELASTICSEARCH_VERSION: z.coerce
		.number()
		.refine((val) => val === 7 || val === 8, { message: 'Version must be 7 or 8' }), // Ensure 7 or 8
});

const kafkaConfigSchema = z.object({
	MAESTRO_KAFKA_ENABLED: z.coerce.boolean().default(false),
	MAESTRO_KAFKA_LYRIC_ANALYSIS_MESSAGE_TOPIC: z.string().optional().default('clinical_data'),
	MAESTRO_KAFKA_LYRIC_ANALYSIS_MESSAGE_DLQ: z.string().optional().default('clinical_data_dlq'),
	MAESTRO_KAFKA_LYRIC_REQUEST_MESSAGE_TOPIC: z.string().optional().default('clinical_index_request'),
	MAESTRO_KAFKA_LYRIC_REQUEST_MESSAGE_DLQ: z.string().optional().default('clinical_index_request_dlq'),
	MAESTRO_KAFKA_SERVERS: z.string().optional(),
	MAESTRO_KAFKA_SONG_ANALYSIS_MESSAGE_TOPIC: z.string().default('song_analysis'),
	MAESTRO_KAFKA_SONG_ANALYSIS_MESSAGE_DLQ: z.string().default('song_analysis_dlq'),
	MAESTRO_KAFKA_SONG_REQUEST_MESSAGE_TOPIC: z.string().default('index_request'),
	MAESTRO_KAFKA_SONG_REQUEST_MESSAGE_DLQ: z.string().default('index_request_dlq'),
});

const stringToJSONSchema = z.string().transform((str, ctx) => {
	try {
		return JSON.parse(str);
	} catch (e) {
		ctx.addIssue({ code: 'custom', message: `Invalid JSON. ${e}` });
		return z.NEVER;
	}
});

const songRepositorySchema = z.object({
	baseUrl: z.string(),
	code: z.string(),
	country: z.string(),
	name: z.string(),
	organization: z.string(),
});

const songConfigSchema = z.object({
	MAESTRO_SONG_INDEXABLE_STUDY_STATES: z.string().default('PUBLISHED'),
	MAESTRO_SONG_INDEX_ANALYSISCENTRIC_ALIAS: z.string().default('analysis_centric'),
	MAESTRO_SONG_INDEX_ANALYSISCENTRIC_ENABLED: z.coerce.boolean().default(false),
	MAESTRO_SONG_INDEX_ANALYSISCENTRIC_NAME: z.string().default('analysis_centric_1'),
	MAESTRO_SONG_INDEX_FILECENTRIC_ALIAS: z.string().default('file_centric'),
	MAESTRO_SONG_INDEX_FILECENTRIC_ENABLED: z.coerce.boolean().default(false),
	MAESTRO_SONG_INDEX_FILECENTRIC_NAME: z.string().default('file_centric_1'),
	MAESTRO_SONG_REPOSITORIES: stringToJSONSchema.pipe(z.array(songRepositorySchema)).optional(),
});

const LyricRepositoryConfig = z.object({
	baseUrl: z.string(),
	categoryId: z.coerce.number(),
	code: z.string(),
	name: z.string(),
});

const lyricConfigSchema = z
	.object({
		MAESTRO_LYRIC_INDEX_ALIAS: z.string().default('clinical_data_1.0'),
		MAESTRO_LYRIC_INDEX_ENABLED: z.coerce.boolean().default(false),
		MAESTRO_LYRIC_INDEX_NAME: z.string().default('clinical_data'),
		MAESTRO_LYRIC_INDEX_VALID_DATA_ONLY: z.coerce.boolean().default(false),
		MAESTRO_LYRIC_REPOSITORIES: stringToJSONSchema.pipe(z.array(LyricRepositoryConfig)).optional(),
	})
	.optional();

const loggerConfigSchema = z.object({
	MAESTRO_LOGGING_LEVEL_ROOT: z.string().default('info'),
});

const envConfig = elasticSearchConfigSchema
	.and(elasticSearchConfigSchema)
	.and(kafkaConfigSchema)
	.and(loggerConfigSchema)
	.and(songConfigSchema)
	.and(lyricConfigSchema);

const envParsed = envConfig.safeParse(process.env);

if (!envParsed.success) {
	console.error(envParsed.error.issues);
	throw new Error('There is an error with the server environment variables.');
}

export const env = envParsed.data;
