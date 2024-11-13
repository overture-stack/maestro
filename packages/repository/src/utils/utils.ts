import type { DataRecordValue, LyricRepositoryConfig, SongRepositoryConfig } from '@overture-stack/maestro-common';

/**
 * Finds the repository by its repository code
 * @param repositories
 * @param repoCode
 * @returns
 */
export const getRepoInformation = (
	repositories: (SongRepositoryConfig | LyricRepositoryConfig)[],
	repoCode: string,
): SongRepositoryConfig | LyricRepositoryConfig | undefined => {
	return repositories.find((repo) => repo.code === repoCode);
};
/**
 * Determines if passed configuration is appropiate for Lyric
 * @param object
 * @returns
 */
export const isLyricConfiguration = (
	object: LyricRepositoryConfig | SongRepositoryConfig,
): object is LyricRepositoryConfig => {
	return object.type === 'LYRIC';
};

/**
 * Determines if passed configuration is appropiate for Song
 * @param object
 * @returns
 */
export const isSongConfiguration = (
	object: LyricRepositoryConfig | SongRepositoryConfig,
): object is SongRepositoryConfig => {
	return object.type === 'SONG';
};

/**
 * Checks if a given value is an array of objects where each object has values that are either
 * strings, numbers, booleans, `null`, or nested objects
 * @param value
 * @returns
 */
export const isArrayOfObjects = (value: unknown): value is Array<Record<string, DataRecordValue>> => {
	return (
		Array.isArray(value) &&
		value.every(
			(item) =>
				typeof item === 'object' &&
				item !== null &&
				Object.values(item).every(
					(val) =>
						typeof val === 'string' ||
						typeof val === 'number' ||
						typeof val === 'boolean' ||
						val === null ||
						typeof val === 'object',
				),
		)
	);
};
