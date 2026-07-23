import { type LyricRepositoryConfig, RepositoryType, type SongRepositoryConfig } from '@overture-stack/maestro-common';

export const isDefined = (item: string | undefined): item is string => {
	return !!item;
};

export const getRepoTopics = (repos: (SongRepositoryConfig | LyricRepositoryConfig)[]) => {
	return repos.map((repo) => repo.kafkaTopic).filter(isDefined);
};

/** The category-identifying fields of a parsed Lyric document message, if present. */
type DocumentMessageIdentity = {
	categoryAlias?: string;
	categoryId?: number | string;
};

/**
 * Returns every repo configured for `topic` whose `categoryId` matches the message's own
 * `categoryId` or `categoryAlias`, by equality not shape. Always enforced for Lyric repos, even
 * when a topic maps to only one: Lyric's publishing isn't under Maestro's control, it may fan out
 * several categories onto one topic while Maestro is only configured to index a subset of them.
 * SONG repos have no category concept, so topic alone is sufficient to route to them.
 *
 * More than one match is a deliberate fan-out, not an error; a caller treating zero matches as
 * unroutable is responsible for that.
 */
export const getMatchingRepos = (
	repos: (SongRepositoryConfig | LyricRepositoryConfig)[],
	topic: string,
	message: DocumentMessageIdentity,
): (SongRepositoryConfig | LyricRepositoryConfig)[] => {
	const candidates = repos.filter((repo) => repo.kafkaTopic === topic);
	const messageCategoryId = message.categoryId === undefined ? undefined : String(message.categoryId);

	return candidates.filter((repo) => {
		if (repo.type !== RepositoryType.LYRIC) {
			return true;
		}
		const configuredValue = String(repo.categoryId);
		return configuredValue === message.categoryAlias || configuredValue === messageCategoryId;
	});
};

const isLyricRepo = (repo: SongRepositoryConfig | LyricRepositoryConfig): repo is LyricRepositoryConfig => {
	return repo.type === RepositoryType.LYRIC;
};

/** The configured `categoryId` (numeric id or alias, verbatim) of every Lyric repo on `topic`, for troubleshooting an unmatched message. */
export const getConfiguredCategoryIds = (
	repos: (SongRepositoryConfig | LyricRepositoryConfig)[],
	topic: string,
): (number | string)[] => {
	return repos
		.filter((repo) => repo.kafkaTopic === topic)
		.filter(isLyricRepo)
		.map((repo) => repo.categoryId);
};
