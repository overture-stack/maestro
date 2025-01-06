import 'dotenv/config';

import { z } from 'zod';

import { logger } from '../utils/logger.js';
import { validateRepositories } from './repositoryConfig.js';

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
		.int()
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

const loggerConfigSchema = z.object({
	MAESTRO_LOGGING_LEVEL: z.string().default('info'),
});

// Create the main schema by intersecting the other schemas and the validated union
const mainSchema = elasticSearchConfigSchema.and(kafkaConfigSchema).and(loggerConfigSchema);

const mainSchemaParsed = mainSchema.safeParse(process.env);

if (!mainSchemaParsed.success) {
	mainSchemaParsed.error.issues.forEach((issue) => {
		logger.error(issue);
	});
	throw new Error('There is an error with the server environment variables.');
}

// Create Repository schemas and validate them
const parsedRepositories = validateRepositories(process.env);

export const env = { ...mainSchemaParsed.data, repositories: parsedRepositories };
