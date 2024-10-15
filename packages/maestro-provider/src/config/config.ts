import type { ElasticSearchConfig, LoggerConfig } from '@overture-stack/maestro-common';

type BindingConfig = {
	topic: string;
	dlq: string;
};

type SongSchemaBindingConfig = {
	analysisMessage: BindingConfig;
	requestMessage: BindingConfig;
};

export type KafkaConfig = {
	dynamicSchemaBinding?: BindingConfig;
	enabled: boolean;
	servers: string;
	songSchemaBinding?: SongSchemaBindingConfig;
};

type RepositoryConfig = {
	code: string;
	country: string;
	name: string;
	organization: string;
	url: string;
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

type SongConfig = {
	indexableStudyStates: string;
	indices: SongIndicesConfig;
	repositories: RepositoryConfig[];
};

export type AppConfig = {
	elasticSearchConfig: ElasticSearchConfig;
	kafka: KafkaConfig;
	logger: LoggerConfig;
	songConfig?: SongConfig;
};
