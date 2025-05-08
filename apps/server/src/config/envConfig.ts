import 'dotenv/config';

import { z } from 'zod';

import { ElasticSearchSupportedVersions } from '@overture-stack/maestro-common';

import { logger } from '../utils/logger.js';
import { validateRepositories } from './repositoryConfig.js';

export const getServerConfig = () => {
	return {
		port: process.env.MAESTRO_SERVER_PORT || 11235,
		nodeEnv: process.env.NODE_ENV || 'development',
		openApiPath: process.env.MAESTRO_OPENAPI_PATH || 'api-docs',
	};
};

const supportedVersionValues = Object.values(ElasticSearchSupportedVersions);

const elasticSearchConfigSchema = z.object({
	MAESTRO_ELASTICSEARCH_CLIENT_BASICAUTH_ENABLED: z.coerce.boolean(),
	MAESTRO_ELASTICSEARCH_CLIENT_BASICAUTH_PASSWORD: z.string().optional(),
	MAESTRO_ELASTICSEARCH_CLIENT_BASICAUTH_USER: z.string().optional(),
	MAESTRO_ELASTICSEARCH_CLIENT_CONNECTION_TIMEOUT: z.coerce.number().optional(),
	MAESTRO_ELASTICSEARCH_CLIENT_DOCS_PER_BULK_REQ_MAX: z.coerce.number().default(5000),
	MAESTRO_ELASTICSEARCH_CLIENT_RETRY_MAX_ATTEMPTS: z.coerce.number().optional(),
	MAESTRO_ELASTICSEARCH_CLIENT_RETRY_WAIT_DURATION_MILLIS: z.coerce.number().optional(),
	MAESTRO_ELASTICSEARCH_NODES: z.string(),
	MAESTRO_ELASTICSEARCH_VERSION: z
		.enum([String(supportedVersionValues[0]), ...supportedVersionValues.slice(1).map(String)])
		.transform((val) => Number(val)),
});

const kafkaConfigSchema = z
	.object({
		MAESTRO_KAFKA_INDEX_REQUEST_TOPIC: z.string().optional(),
		MAESTRO_KAFKA_INDEX_REQUEST_DLQ: z.string().optional(),
		MAESTRO_KAFKA_GROUP_ID: z.string().optional(),
		MAESTRO_KAFKA_SERVER: z.string(),
	})
	.optional();

// Pino logger levels (https://github.com/pinojs/pino/blob/main/docs/api.md#level)
const LogLeveOptions = ['trace', 'debug', 'info', 'warn', 'error', 'fatal'] as const;

const loggerConfigSchema = z.object({
	MAESTRO_LOGGING_LEVEL: z.enum(LogLeveOptions).default('info'),
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
