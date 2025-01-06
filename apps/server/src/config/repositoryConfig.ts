import { z } from 'zod';

import { logger } from '../utils/logger.js';

export const repositoryTypes = z.enum(['LYRIC', 'SONG']);

const definitionBaseRepositorySchema = z.object({
	BASE_URL: z.string().url(),
	CODE: z.string(),
	NAME: z.string(),
	PAGINATION_SIZE: z.coerce.number().optional(),
	INDEX_NAME: z.string(),
});

const definitionLyricRepositorySchema = z.object({
	TYPE: z.literal(repositoryTypes.Values.LYRIC),
	LYRIC_VALID_DATA_ONLY: z.coerce.boolean().default(true),
	LYRIC_CATEGORY_ID: z.coerce.number(),
});
export const lyricSchemaDefinition = definitionBaseRepositorySchema.and(definitionLyricRepositorySchema);
const isLyricRepository = (data: unknown): data is z.infer<typeof lyricSchemaDefinition> => {
	return lyricSchemaDefinition.safeParse(data).success;
};

const definitionSongRepositorySchema = z.object({
	TYPE: z.literal(repositoryTypes.Values.SONG),
	SONG_INDEXABLE_STUDY_STATES: z.string().default('PUBLISHED'),
	SONG_ANALYSIS_CENTRIC_ENABLED: z.coerce.boolean().default(true),
	SONG_ORGANIZATION: z.string().optional(),
	SONG_COUNTRY: z.string().optional(),
});
export const songSchemaDefinition = definitionBaseRepositorySchema.and(definitionSongRepositorySchema);
const isSongRepository = (data: unknown): data is z.infer<typeof songSchemaDefinition> => {
	return songSchemaDefinition.safeParse(data).success;
};

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
			SONG_ANALYSIS_CENTRIC_ENABLED: env[`${baseKeyPrefix}_SONG_ANALYSIS_CENTRIC_ENABLED`],
			SONG_ORGANIZATION: env[`${baseKeyPrefix}_SONG_ORGANIZATION`],
			SONG_COUNTRY: env[`${baseKeyPrefix}_SONG_COUNTRY`],
		};

		try {
			const parsed = repositorySchema.parse(repoData);

			if (isLyricRepository(parsed) || isSongRepository(parsed)) {
				resultParsedRepositories.push(parsed);
			}
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
