import {
	type IElasticsearchService,
	initializeLogger,
	type RepositoryIndexingOperations,
} from '@overture-stack/maestro-common';
import type { MaestroProviderConfig } from '@overture-stack/maestro-common/dist/types/config.js';
import { clientProvider } from '@overture-stack/maestro-indexer-client';

import { api } from '../api/api.js';

/**
 * Interface representing a provider for indexing operations
 */
export interface IMaestroProvider {
	/**
	 * The API for repository indexing operations.
	 */
	api: RepositoryIndexingOperations;
	/**
	 * The Elasticsearch service implementation payload.
	 */
	payload: IElasticsearchService;
}

/**
 * Creates The Maestro Provider based on the provided configuration
 * @param config The configuration object for initializing the Maestro provider
 * @returns The Maestro Provider object containing API operations and an Elasticsearch service instance
 */
export const MaestroProvider = (config: MaestroProviderConfig): IMaestroProvider => {
	initializeLogger(config.logger);
	const indexerProvider = clientProvider(config.elasticSearchConfig);

	const apiOperations = api(config, indexerProvider);

	return {
		api: apiOperations,
		payload: indexerProvider,
	};
};
