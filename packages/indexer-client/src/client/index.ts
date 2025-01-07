import { type DataRecordNested, ElasticSearchConfig, ElasticsearchService } from '@overture-stack/maestro-common';

import { es7 } from './v7/client.js';
import { es8 } from './v8/client.js';

export type ESVersion = '7' | '8';

/**
 * Provides an instance of `ElasticsearchService` based on the specified Elasticsearch version in the configuration
 *
 * @param elasticSearchConfig The configuration object for Elasticsearch, including the version and custom settings
 * @returns An implementation of `ElasticsearchService` that matches the specified version
 * @throws Will throw an error if the provided Elasticsearch version is not supported
 */
export const clientProvider = (elasticSearchConfig: ElasticSearchConfig): ElasticsearchService => {
	let service: ElasticsearchService;

	if (elasticSearchConfig.version === 7) {
		service = es7(elasticSearchConfig);
	} else if (elasticSearchConfig.version === 8) {
		service = es8(elasticSearchConfig);
	} else {
		throw new Error('Unsupported Elasticsearch version');
	}

	return {
		addData: (index: string, data: DataRecordNested) => service.addData(index, data),
		bulkUpsert: (index: string, data: DataRecordNested[]) => service.bulkUpsert(index, data),
		createIndex: (index: string) => service.createIndex(index),
		deleteData: (index: string, id: string) => service.deleteData(index, id),
		ping: () => service.ping(),
		updateData: (index: string, id: string, data: DataRecordNested) => service.updateData(index, id, data),
	};
};
