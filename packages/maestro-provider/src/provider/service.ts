import type { Config, IElasticsearchService } from '@overture-stack/maestro-common';
import { clientProvider } from '@overture-stack/maestro-indexer-client';

export type MaestroProvider = {
	indexerProvider: IElasticsearchService;
};

export const MaestroProvider = (config: Config): MaestroProvider => {
	const indexerProvider = clientProvider(config.elasticSearchConfig);
	return {
		indexerProvider: indexerProvider,
	};
};
