import { Client } from 'es8';

import type { DataRecordValue, IElasticsearchService, IndexData, IndexResult } from '@overture-stack/maestro-common';

import { createIndexIfNotExists, deleteData, indexData, ping, updateData } from './operations.js';

/**
 *  Creates an instance of the Elasticsearch service for version 8.
 *
 * This function initializes an Elasticsearch client using the provided node URL
 * and returns an object that implements the `IElasticsearchService` interface.
 * The returned object includes methods for creating an index, indexing data,
 * checking the connection, updating data, and deleting data.
 *
 * @param {string} nodeUrl The URL of the Elasticsearch node to connect to
 * @returns {IElasticsearchService} An object implementing the `IElasticsearchService` interface, providing methods to interact with Elasticsearch
 */
export const es8 = (nodeUrl: string): IElasticsearchService => {
	const client = new Client({ node: nodeUrl });

	return {
		async createIndex(index: string): Promise<boolean> {
			return createIndexIfNotExists(client, index);
		},

		async indexData(index: string, data: IndexData): Promise<IndexResult> {
			return indexData(client, index, data);
		},

		async ping(): Promise<boolean> {
			return ping(client);
		},

		async updateData(index: string, id: string, data: Record<string, DataRecordValue>): Promise<IndexResult> {
			return updateData(client, index, id, data);
		},

		async deleteData(index: string, id: string): Promise<IndexResult> {
			return deleteData(client, index, id);
		},
	};
};
