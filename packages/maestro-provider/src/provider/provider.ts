import {
	type ElasticsearchService,
	type MaestroProviderConfig,
	type RepositoryIndexingOperations,
	setLogger,
} from '@overture-stack/maestro-common';
import { clientProvider } from '@overture-stack/maestro-indexer-client';
import { initializeConsumer } from '@overture-stack/maestro-kafka';

import { api } from '../api/api.js';

/**
 * Interface representing a provider for indexing operations
 */
export interface MaestroProvider {
	/**
	 * The API for repository indexing operations.
	 */
	api?: RepositoryIndexingOperations;
	/**
	 * The Elasticsearch service implementation payload.
	 */
	payload: ElasticsearchService;
}

/**
 * Creates The Maestro Provider based on the provided configuration
 * @param config The configuration object for initializing the Maestro provider
 * @returns The Maestro Provider object containing API operations and an Elasticsearch service instance
 */
export const initializeMaestroProvider = (config: MaestroProviderConfig): MaestroProvider => {
	if (config.logger) {
		setLogger(config.logger);
	}
	const indexerProvider = clientProvider(config.elasticSearchConfig);

	// Initialize Kafka consumer if configured
	if (config.kafka?.server && config.repositories) {
		initializeConsumer({ kafkaConfig: config.kafka, repositories: config.repositories, indexerProvider });
	}

	return {
		api: config.repositories ? api(config.repositories, indexerProvider) : undefined,
		payload: indexerProvider,
	};
};
