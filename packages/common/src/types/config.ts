import type { ElasticSearchConfig } from './clientConfig.js';
import { ConsoleLike } from './logger.js';
interface BindingConfig {
	dlq?: string;
	topic?: string;
}
export interface KafkaConfig {
	requestBinding?: BindingConfig;
	brokers?: string;
	groupId?: string;
}

export const RepositoryType = {
	SONG: 'SONG',
	LYRIC: 'LYRIC',
} as const;

type ValueOf<T> = T[keyof T];
export type RepositoryType = ValueOf<typeof RepositoryType>;

export interface RepositoryConfig {
	baseUrl: string;
	code: string;
	name: string;
	paginationSize?: number;
	kafkaTopic?: string;
	kafkaDlq?: string;
}

interface IndexConfig {
	indexName: string;
}

export const IndexingMode = {
	fileCentric: 'file',
	analysisCentric: 'analysis',
} as const;

export type IndexingMode = ValueOf<typeof IndexingMode>;

export const IndexableState = {
	PUBLISHED: 'PUBLISHED',
	UNPUBLISHED: 'UNPUBLISHED',
	SUPPRESSED: 'SUPPRESSED',
} as const;

export type IndexableState = (typeof IndexableState)[keyof typeof IndexableState];

interface SongIndexConfig extends IndexConfig {
	indexingMode: IndexingMode;
	indexableStudyStates: IndexableState[];
}

interface LyricIndexConfig extends IndexConfig {
	validDataOnly: boolean;
}

export interface LyricRepositoryConfig extends RepositoryConfig, LyricIndexConfig {
	/**
	 * The Lyric category this repository indexes, or an alias for it. Matched against an incoming
	 * message's `categoryId`/`categoryAlias` by equality, not shape, so a numeric-looking alias
	 * is never confused with a plain id.
	 */
	categoryId: number | string;
	type: typeof RepositoryType.LYRIC;
}

export interface SongRepositoryConfig extends RepositoryConfig, SongIndexConfig {
	country?: string;
	organization?: string;
	type: typeof RepositoryType.SONG;
}

export interface MaestroProviderConfig {
	elasticSearchConfig: ElasticSearchConfig;
	kafka?: KafkaConfig;
	logger?: ConsoleLike;
	repositories?: (LyricRepositoryConfig | SongRepositoryConfig)[];
}
