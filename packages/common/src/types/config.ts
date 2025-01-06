import type { ElasticSearchConfig } from './clientConfig.js';
import type { LoggerConfig } from './logger.js';
interface BindingConfig {
	dlq: string;
	topic: string;
}
interface SchemaBindingConfig {
	analysisMessage: BindingConfig;
	requestMessage: BindingConfig;
}
export interface KafkaConfig {
	enabled: boolean;
	lyricSchemaBinding?: SchemaBindingConfig;
	servers?: string;
	songSchemaBinding?: SchemaBindingConfig;
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
	type: RepositoryType;
}

interface IndexConfig {
	indexName: string;
}

interface SongIndexConfig extends IndexConfig {
	indexableStudyStates: string;
	analysisCentricEnabled: boolean;
}

interface LyricIndexConfig extends IndexConfig {
	validDataOnly: boolean;
}

export interface LyricRepositoryConfig extends RepositoryConfig, LyricIndexConfig {
	categoryId: number;
}

export interface SongRepositoryConfig extends RepositoryConfig, SongIndexConfig {
	country?: string;
	organization?: string;
}

export interface MaestroProviderConfig {
	elasticSearchConfig: ElasticSearchConfig;
	kafka?: KafkaConfig;
	logger?: LoggerConfig;
	repositories?: (LyricRepositoryConfig | SongRepositoryConfig)[];
}
