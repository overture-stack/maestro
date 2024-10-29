import { Client } from 'es8';

import type {
	DataRecordValue,
	ElasticSearchConfig,
	IElasticsearchService,
	IndexData,
	IndexResult,
} from '@overture-stack/maestro-common';

import { getAuth } from '../../common/config.js';
import { createIndexIfNotExists, deleteData, indexData, ping, updateData } from './operations.js';

/**
 *  Creates an instance of the Elasticsearch service for version 8.
 *
 * This function initializes an Elasticsearch client using the provided configuration
 * and returns an object that implements the `IElasticsearchService` interface.
 * The returned object includes methods for creating an index, indexing data,
 * checking the connection, updating data, and deleting data.
 *
 * @param {ElasticSearchConfig} config The configuration of the Elasticsearch to connect to
 * @returns {IElasticsearchService} An object implementing the `IElasticsearchService` interface, providing methods to interact with Elasticsearch
 */
export const es8 = (config: ElasticSearchConfig): IElasticsearchService => {
	if (config.version !== 8) {
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
		async addData(index: string, data: IndexData): Promise<IndexResult> {
			return indexData(client, index, data);
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

		async updateData(index: string, id: string, data: Record<string, DataRecordValue>): Promise<IndexResult> {
			return updateData(client, index, id, data);
		},
	};
};
