import {
	type CreateBulkRequest,
	type DataRecordNested,
	type DeleteBulkRequest,
	ElasticSearchConfig,
	ElasticsearchService,
	ElasticSearchSupportedVersions,
	type UpdateBulkRequest,
	type UpsertBulkRequest,
} from '@overture-stack/maestro-common';

import { es7 } from './v7/client.js';
import { es8 } from './v8/client.js';

/**
 * Provides an instance of `ElasticsearchService` based on the specified Elasticsearch version in the configuration
 *
 * @param elasticSearchConfig The configuration object for Elasticsearch, including the version and custom settings
 * @returns An implementation of `ElasticsearchService` that matches the specified version
 * @throws Will throw an error if the provided Elasticsearch version is not supported
 */
export const clientProvider = (elasticSearchConfig: ElasticSearchConfig): ElasticsearchService => {
	let service: ElasticsearchService;

	if (elasticSearchConfig.version === ElasticSearchSupportedVersions.V7) {
		service = es7(elasticSearchConfig);
	} else if (elasticSearchConfig.version === ElasticSearchSupportedVersions.V8) {
		service = es8(elasticSearchConfig);
	} else {
		throw new Error('Unsupported Elasticsearch version');
	}

	return {
		addData: (index: string, data: DataRecordNested) => service.addData(index, data),
		bulk: (index: string, request: (CreateBulkRequest | UpdateBulkRequest | DeleteBulkRequest | UpsertBulkRequest)[]) =>
			service.bulk(index, request),
		createIndex: (index: string) => service.createIndex(index),
		deleteData: (index: string, id: string) => service.deleteData(index, id),
		ping: () => service.ping(),
		updateData: (index: string, id: string, data: DataRecordNested) => service.updateData(index, id, data),
	};
};
