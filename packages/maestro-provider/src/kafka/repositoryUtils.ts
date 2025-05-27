import type { LyricRepositoryConfig, SongRepositoryConfig } from '@overture-stack/maestro-common';

export const isDefined = (item: string | undefined): item is string => {
	return !!item;
};

export const getRepoTopics = (repos: (SongRepositoryConfig | LyricRepositoryConfig)[]) => {
	return repos.map((repo) => repo.kafkaTopic).filter(isDefined);
};

export const getRepoByTopic = (repos: (SongRepositoryConfig | LyricRepositoryConfig)[], topic: string) => {
	return repos.find((repo) => repo.kafkaTopic === topic);
};
