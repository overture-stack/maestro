import 'dotenv/config';

import { z, type ZodRawShape } from 'zod';

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

const loggerConfigSchema = z.object({
	MAESTRO_LOGGING_LEVEL_ROOT: z.string().default('info'),
});

export const repositoryTypes = z.enum(['LYRIC', 'SONG']);

const definitionBaseRepositorySchema = z.object({
	BASE_URL: z.string().url(),
	CODE: z.string(),
	NAME: z.string(),
	PAGINATION_SIZE: z.coerce.number().optional(),
	TYPE: repositoryTypes,
	INDEX_NAME: z.string(),
});

const definitionLyricRepositorySchema = z.object({
	TYPE: z.literal(repositoryTypes.Values.LYRIC),
	LYRIC_VALIDATE_DATA_ONLY: z.boolean().default(true),
	LYRIC_CATEGORY_ID: z.coerce.number(),
});

const definitionSongRepositorySchema = z.object({
	TYPE: z.literal(repositoryTypes.Values.SONG),
	SONG_INDEXABLE_STUDY_STATES: z.string().default('PUBLISHED'),
	SONG_ANALYSIS_CENTRIC_ENABLED: z.boolean().default(true),
	SONG_ORGANIZATION: z.string().optional(),
	SONG_COUNTRY: z.string().optional(),
});

// Function to generate the schema for a dynamic number of repositories
function createDynamicBaseRepositorySchema(count: number) {
	const schemas: ZodRawShape = {};

	for (let i = 0; i < count; i++) {
		schemas[`MAESTRO_REPOSITORIES_${i}_BASE_URL`] = definitionBaseRepositorySchema.shape.BASE_URL;
		schemas[`MAESTRO_REPOSITORIES_${i}_CODE`] = definitionBaseRepositorySchema.shape.CODE;
		schemas[`MAESTRO_REPOSITORIES_${i}_NAME`] = definitionBaseRepositorySchema.shape.NAME;
		schemas[`MAESTRO_REPOSITORIES_${i}_PAGINATION_SIZE`] = definitionBaseRepositorySchema.shape.PAGINATION_SIZE;
		schemas[`MAESTRO_REPOSITORIES_${i}_TYPE`] = definitionBaseRepositorySchema.shape.TYPE;
		schemas[`MAESTRO_REPOSITORIES_${i}_INDEX_NAME`] = definitionBaseRepositorySchema.shape.INDEX_NAME;
	}
	return z.object(schemas);
}

// Function to generate the schema for Lyric configuration for a dynamic number of repositories
function createDynamicCustomLyricRepositorySchema(count: number) {
	const schemas: ZodRawShape = {};

	for (let i = 0; i < count; i++) {
		schemas[`MAESTRO_REPOSITORIES_${i}_TYPE`] = definitionLyricRepositorySchema.shape.TYPE;
		schemas[`MAESTRO_REPOSITORIES_${i}_LYRIC_VALIDATE_DATA_ONLY`] =
			definitionLyricRepositorySchema.shape.LYRIC_VALIDATE_DATA_ONLY;
		schemas[`MAESTRO_REPOSITORIES_${i}_LYRIC_CATEGORY_ID`] = definitionLyricRepositorySchema.shape.LYRIC_CATEGORY_ID;
	}
	return z.object(schemas);
}

// Function to generate the schema for Lyric configuration for a dynamic number of repositories
function createDynamicCustomSongRepositorySchema(count: number) {
	const schemas: ZodRawShape = {};

	for (let i = 0; i < count; i++) {
		schemas[`MAESTRO_REPOSITORIES_${i}_TYPE`] = definitionSongRepositorySchema.shape.TYPE;
		schemas[`MAESTRO_REPOSITORIES_${i}_SONG_INDEXABLE_STUDY_STATES`] =
			definitionSongRepositorySchema.shape.SONG_INDEXABLE_STUDY_STATES;
		schemas[`MAESTRO_REPOSITORIES_${i}_SONG_ANALYSIS_CENTRIC_ENABLED`] =
			definitionSongRepositorySchema.shape.SONG_ANALYSIS_CENTRIC_ENABLED;
		schemas[`MAESTRO_REPOSITORIES_${i}_SONG_ORGANIZATION`] = definitionSongRepositorySchema.shape.SONG_ORGANIZATION;
		schemas[`MAESTRO_REPOSITORIES_${i}_SONG_COUNTRY`] = definitionSongRepositorySchema.shape.SONG_COUNTRY;
	}
	return z.object(schemas);
}

// Define the number of repositories based on the environment variables present
const getRepoCount = (): number => {
	let count = 0;
	while (process.env[`MAESTRO_REPOSITORIES_${count}_BASE_URL`]) {
		count++;
	}
	return count;
};

// Create Repository schemas
const repoCount = getRepoCount();
const baseRepositorySchema = createDynamicBaseRepositorySchema(repoCount);

// Create the main schema by intersecting the other schemas and the validated union
const mainSchema = elasticSearchConfigSchema.and(kafkaConfigSchema).and(loggerConfigSchema).and(baseRepositorySchema);

const mainSchemaParsed = mainSchema.safeParse(process.env);

if (!mainSchemaParsed.success) {
	console.error(mainSchemaParsed.error.issues);
	throw new Error('There is an error with the server environment variables.');
}

// Create the Schemas for Song/Lyric repository properties
const songDynamicConfigSchema = createDynamicCustomSongRepositorySchema(repoCount);
const lyricDynamicConfigSchema = createDynamicCustomLyricRepositorySchema(repoCount);

const songOrLyricSchema = songDynamicConfigSchema.or(lyricDynamicConfigSchema);

const songOrLyricParsed = songOrLyricSchema.safeParse(process.env);

if (!songOrLyricParsed.success) {
	songOrLyricParsed.error.issues.forEach((issue) => {
		if (issue.code === 'invalid_union') {
			issue.unionErrors.forEach((ue) => console.error(ue.errors));
		}
	});
	throw new Error('There is an error with the server environment variables.');
}

const repositoriesParsed = Object.entries({ ...mainSchemaParsed.data, ...songOrLyricParsed.data }).reduce<
	Record<string, unknown>[]
>((acc, [key, value]) => {
	if (key.startsWith('MAESTRO_REPOSITORIES_')) {
		const parts = key.split('_');
		if (parts[2]) {
			const index = parseInt(parts[2], 10);
			const newKey = parts.slice(3).join('_');
			if (acc[index]) {
				acc[index][newKey] = value;
			} else {
				acc[index] = { [newKey]: value };
			}
		}
	}
	return acc;
}, []);

export const lyricSchemaDefinition = definitionBaseRepositorySchema.and(definitionLyricRepositorySchema);
export const songSchemaDefinition = definitionBaseRepositorySchema.and(definitionSongRepositorySchema);

const parsedRepositories: (z.infer<typeof lyricSchemaDefinition> | z.infer<typeof songSchemaDefinition>)[] = [];

const isLyricRepository = (data: unknown): data is z.infer<typeof lyricSchemaDefinition> => {
	return lyricSchemaDefinition.safeParse(data).success;
};

const isSongRepository = (data: unknown): data is z.infer<typeof songSchemaDefinition> => {
	return songSchemaDefinition.safeParse(data).success;
};

repositoriesParsed.forEach((reps) => {
	const parsed = lyricSchemaDefinition.or(songSchemaDefinition).safeParse(reps);
	if (isLyricRepository(parsed.data)) {
		parsedRepositories.push(parsed.data);
	} else if (isSongRepository(parsed.data)) {
		parsedRepositories.push(parsed.data);
	}
});

export const env = { ...mainSchemaParsed.data, repositories: parsedRepositories };
