import { Client } from 'es7';

import type {
	CreateBulkRequest,
	DataRecordNested,
	DeleteBulkRequest,
	ElasticSearchConfig,
	ElasticsearchService,
	IndexResult,
	UpdateBulkRequest,
	UpsertBulkRequest,
} from '@overture-stack/maestro-common';

import { getAuth } from '../../common/config.js';
import { bulk, createIndexIfNotExists, deleteData, indexData, ping, updateData } from './operations.js';

/**
 *  Creates an instance of the Elasticsearch service for version 7.
 *
 * This function initializes an Elasticsearch client using the provided configuration
 * and returns an object that implements the `ElasticsearchService` interface.
 * The returned object includes methods for creating an index, indexing data,
 * checking the connection, updating data, and deleting data.
 *
 * @param {ElasticSearchConfig} config The configuration of the Elasticsearch to connect to
 * @returns {ElasticsearchService} An object implementing the `ElasticsearchService` interface, providing methods to interact with Elasticsearch
 */
export const es7 = (config: ElasticSearchConfig): ElasticsearchService => {
	if (config.version !== 7) {
		throw Error('Invalid Client Configuration');
	}

	const auth = getAuth(config.basicAuth);

	const client = new Client({
		node: config.nodes,
		auth,
		requestTimeout: config.connectionTimeOut,
		maxRetries: config.retry?.retryMaxAttempts,
	});

	return {
		async addData(index: string, data: DataRecordNested): Promise<IndexResult> {
			return indexData(client, index, data);
		},

		async bulk(
			index: string,
			request: (CreateBulkRequest | UpdateBulkRequest | DeleteBulkRequest | UpsertBulkRequest)[],
		): Promise<IndexResult> {
			return bulk(client, index, request);
		},

		async createIndex(index: string): Promise<boolean> {
			return createIndexIfNotExists(client, index);
		},

		async deleteData(index: string, id: string): Promise<IndexResult> {
			return deleteData(client, index, id);
		},

		async ping(): Promise<boolean> {
			return ping(client);
		},

		async updateData(index: string, id: string, data: DataRecordNested): Promise<IndexResult> {
			return updateData(client, index, id, data);
		},
	};
};
