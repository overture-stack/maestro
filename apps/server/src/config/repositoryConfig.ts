import { z } from 'zod';

import { IndexableState, IndexingMode } from '@overture-stack/maestro-common';

import { logger } from '../utils/logger.js';

export const repositoryTypes = z.enum(['LYRIC', 'SONG']);

const definitionBaseRepositorySchema = z.object({
	BASE_URL: z.string().url(),
	CODE: z.string(),
	NAME: z.string(),
	PAGINATION_SIZE: z.coerce.number().optional(),
	INDEX_NAME: z
		.string()
		.refine((value) => !/[<" \\/,|>?*]/.test(value), {
			// criteria: https://www.elastic.co/guide/en/elasticsearch/reference/7.17/indices-create-index.html
			message: `INDEX_NAME cannot include characters '<', '"', space character, '\\' , '/', ',', '|', '>', '?', or '*'`,
		})
		.refine((value) => !/^[-_+]/.test(value), {
			message: 'INDEX_NAME cannot start with -, _, or +',
		})
		.refine((value) => value !== '.' && value !== '..', {
			message: 'INDEX_NAME cannot be . or ..',
		})
		.refine((value) => /^[^A-Z]*$/.test(value), {
			message: 'INDEX_NAME must be lowercase and contain only alphanumeric characters',
		})
		.refine((value) => Buffer.byteLength(value, 'utf-8') <= 255, {
			message: 'INDEX_NAME cannot be longer than 255 bytes',
		}),
	KAFKA_DOCUMENT_UPDATE_TOPIC: z.string().optional(),
	KAFKA_DOCUMENT_UPDATE_DLQ: z.string().optional(),
});

const definitionLyricRepositorySchema = z.object({
	TYPE: z.literal(repositoryTypes.Values.LYRIC),
	LYRIC_VALID_DATA_ONLY: z.coerce.boolean().default(true),
	LYRIC_CATEGORY_ID: z.coerce.number(),
});
export const lyricSchemaDefinition = definitionBaseRepositorySchema.and(definitionLyricRepositorySchema);

export const IndexingModeSchema = z.enum([IndexingMode.fileCentric, IndexingMode.analysisCentric]);

export const IndexableStateEnum = z.enum([
	IndexableState.PUBLISHED,
	IndexableState.UNPUBLISHED,
	IndexableState.SUPPRESSED,
]);

const definitionSongRepositorySchema = z.object({
	TYPE: z.literal(repositoryTypes.Values.SONG),
	SONG_INDEXABLE_STUDY_STATES: z
		.string()
		.default(IndexableState.PUBLISHED)
		.transform((val) => val.split(',').map((s) => s.trim()))
		.pipe(z.array(IndexableStateEnum)),
	SONG_INDEXING_MODE: IndexingModeSchema,
	SONG_ORGANIZATION: z.string().optional(),
	SONG_COUNTRY: z.string().optional(),
});
export const songSchemaDefinition = definitionBaseRepositorySchema.and(definitionSongRepositorySchema);

// Define the number of repositories based on the environment variables present
const getRepoCount = (): number => {
	let count = 0;
	while (process.env[`MAESTRO_REPOSITORIES_${count}_BASE_URL`]) {
		count++;
	}
	return count;
};

/**
 * Search through the passed environment variables for any related to repositories and perform validation
 * @param env Environment variables
 * @returns an array of valid repositories
 * @throws an error if repository is invalid
 */
export const validateRepositories = (env: NodeJS.ProcessEnv) => {
	const repositorySchema = definitionBaseRepositorySchema.and(
		z.discriminatedUnion('TYPE', [definitionLyricRepositorySchema, definitionSongRepositorySchema]),
	);

	const resultParsedRepositories: (z.infer<typeof lyricSchemaDefinition> | z.infer<typeof songSchemaDefinition>)[] = [];

	const repoCount = getRepoCount();

	// Loop through the repositories found
	for (let i = 0; i < repoCount; i++) {
		// Collect the environment variables for the repository
		const baseKeyPrefix = `MAESTRO_REPOSITORIES_${i}`;
		const repoData = {
			BASE_URL: env[`${baseKeyPrefix}_BASE_URL`],
			CODE: env[`${baseKeyPrefix}_CODE`],
			NAME: env[`${baseKeyPrefix}_NAME`],
			PAGINATION_SIZE: env[`${baseKeyPrefix}_PAGINATION_SIZE`],
			INDEX_NAME: env[`${baseKeyPrefix}_INDEX_NAME`],
			TYPE: env[`${baseKeyPrefix}_TYPE`],
			LYRIC_CATEGORY_ID: env[`${baseKeyPrefix}_LYRIC_CATEGORY_ID`],
			LYRIC_VALID_DATA_ONLY: env[`${baseKeyPrefix}_LYRIC_VALID_DATA_ONLY`],
			SONG_INDEXABLE_STUDY_STATES: env[`${baseKeyPrefix}_SONG_INDEXABLE_STUDY_STATES`],
			SONG_INDEXING_MODE: env[`${baseKeyPrefix}_SONG_INDEXING_MODE`],
			SONG_ORGANIZATION: env[`${baseKeyPrefix}_SONG_ORGANIZATION`],
			SONG_COUNTRY: env[`${baseKeyPrefix}_SONG_COUNTRY`],
			KAFKA_DOCUMENT_UPDATE_TOPIC: env[`${baseKeyPrefix}_KAFKA_DOCUMENT_UPDATE_TOPIC`],
			KAFKA_DOCUMENT_UPDATE_DLQ: env[`${baseKeyPrefix}_KAFKA_DOCUMENT_UPDATE_DLQ`],
		};

		try {
			const parsed = repositorySchema.parse(repoData);
			logger.info(`Configuring repository: ${parsed.CODE} (${parsed.TYPE}) at ${parsed.BASE_URL}`);
			resultParsedRepositories.push(parsed);
		} catch (error) {
			if (error instanceof z.ZodError) {
				error.issues.forEach((issue) => {
					logger.error(`Validation failed for repository ${baseKeyPrefix}`, issue);
				});
			}
			throw new Error('There is an error with the server environment variables.');
		}
	}
	return resultParsedRepositories;
};
