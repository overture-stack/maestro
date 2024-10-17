import type { ElasticSearchConfig } from './clientConfig.js';
import type { LoggerConfig } from './logger.js';

type BindingConfig = {
	topic: string;
	dlq: string;
};

type SchemaBindingConfig = {
	analysisMessage: BindingConfig;
	requestMessage: BindingConfig;
};

export type KafkaConfig = {
	lyricSchemaBinding?: SchemaBindingConfig;
	enabled: boolean;
	servers: string;
	songSchemaBinding?: SchemaBindingConfig;
};

type SongRepositoryConfig = {
	baseUrl: string;
	code: string;
	country: string;
	name: string;
	organization: string;
};

type IndexConfig = {
	alias: string;
	enabled: boolean;
	name: string;
};

type SongIndicesConfig = {
	analysisCentric: IndexConfig;
	fileCentric: IndexConfig;
};

export type SongConfig = {
	indexableStudyStates: string;
	indices: SongIndicesConfig;
	repositories: SongRepositoryConfig[];
};

type LyricRepositoryConfig = {
	baseUrl: string;
	categoryId: number;
	code: string;
	name: string;
};

export type LyricConfig = {
	indexValidDataOnly: boolean;
	index: IndexConfig;
	repositories: LyricRepositoryConfig[];
};

export type Config = {
	elasticSearchConfig: ElasticSearchConfig;
	kafka?: KafkaConfig;
	logger?: LoggerConfig;
	songConfig?: SongConfig;
	lyricConfig?: LyricConfig;
};
