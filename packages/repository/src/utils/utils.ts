import type { DataRecordValue, LyricRepositoryConfig, SongRepositoryConfig } from '@overture-stack/maestro-common';

/**
 * Finds the repository by its repository code.
 * Returns `undefined` if the repository with the given code is not found.
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
