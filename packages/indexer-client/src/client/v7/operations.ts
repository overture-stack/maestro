import type { Client } from 'es7';

import {
	DataRecordValue,
	IndexData,
	IndexResult,
	sanitize_index_name,
	type FailureData,
} from '@overture-stack/maestro-common';

/**
 * Creates an index in Elasticsearch if it does not already exist
 *
 * This function checks if the specified index exists in Elasticsearch
 * If the index does not exist, it creates the index
 * If the index already exists, no action is taken
 *
 * @param client An instance of the Elasticsearch `Client` used to perform the indexing operation
 * @param index The name of the Elasticsearch index to create
 * @returns `true` if the index already exists, `false` if doesn't exist
 */
export const createIndexIfNotExists = async (client: Client, index: string): Promise<boolean> => {
	const sanitizedIndex = sanitize_index_name(index);
	const exists = await client.indices.exists({ index: sanitizedIndex });
	if (!exists.body) {
		await client.indices.create({ index: sanitizedIndex });
		console.log(`Index ${sanitizedIndex} created.`);
	} else {
		console.log(`Index ${sanitizedIndex} already exists.`);
	}
	return exists.body;
};

/**
 * Indexes data into a specified Elasticsearch index
 *
 * @param client An instance of the Elasticsearch `Client` used to perform the indexing operation
 * @param index The name of the Elasticsearch index where the document will be indexed
 * @param input
 * @param input.id The unique identifier for the document to be indexed
 * @param input.data The actual data to be stored in the document
 * @param input.entityName The name of the entity the document belongs to
 * @param input.organization The organization associated with the document
 * @returns A promise that resolves to a `IndexResult`, containing metadata about the operation result
 */
export const indexData = async (
	client: Client,
	index: string,
	{ id, data, entityName, organization }: IndexData,
): Promise<IndexResult> => {
	const sanitizedIndex = sanitize_index_name(index);
	const response = await client.index({
		index: sanitizedIndex,
		id,
		body: {
			data,
			entityName,
			organization,
		},
	});
	console.log('Document indexed:', JSON.stringify(response));

	let successful = false;
	const failureData: FailureData = {};
	if (response.body.result === 'created' || response.body.result === 'updated') {
		successful = true;
	} else {
		failureData[id] = [response.body.result];
	}

	return {
		indexName: index,
		successful,
		failureData,
	};
};

/**
 *  Updates an existing document in a specified Elasticsearch index
 * @param client An instance of the Elasticsearch `Client` used to perform the update operation
 * @param index The name of the Elasticsearch index where the document is located
 * @param input
 * @param input.id The unique identifier of the document to be updated
 * @param input.data A partial set of data fields to update in the document
 * @returns A promise that resolves to a `IndexResult`, containing metadata about the operation result
 */
export const updateData = async (
	client: Client,
	index: string,
	{ id, data }: { id: string; data: Record<string, DataRecordValue> },
): Promise<IndexResult> => {
	const sanitizedIndex = sanitize_index_name(index);
	const response = await client.update({
		index: sanitizedIndex,
		id,
		body: {
			doc: { data },
		},
	});
	console.log('Document updated:', JSON.stringify(response));

	let successful = false;
	const failureData: FailureData = {};
	if (response.body.result === 'created' || response.body.result === 'updated') {
		successful = true;
	} else {
		failureData[id] = [response.body.result];
	}

	return {
		indexName: index,
		successful,
		failureData,
	};
};

/**
 * Deletes a document from a specified Elasticsearch index
 * @param client An instance of the Elasticsearch `Client` used to perform the delete operation
 * @param index The name of the Elasticsearch index from which the document will be deleted
 * @param id The unique identifier of the document to be deleted
 * @returns A promise that resolves to a `IndexResult`, containing metadata about the operation result
 */
export const deleteData = async (client: Client, index: string, id: string): Promise<IndexResult> => {
	const sanitizedIndex = sanitize_index_name(index);
	const response = await client.delete({
		index: sanitizedIndex,
		id,
	});
	console.log('Document deleted:', JSON.stringify(response));

	let successful = false;
	const failureData: FailureData = {};
	if (response.body.result === 'deleted') {
		successful = true;
	} else {
		failureData[id] = [response.body.result];
	}

	return {
		indexName: index,
		successful,
		failureData,
	};
};

/**
 * Checks the connection to the Elasticsearch server.
 *
 * This function sends a ping request to the specified Elasticsearch client
 * to determine if the server is reachable. It returns a boolean indicating
 * the success of the connection. A successful ping means that the server is
 * responding to requests.
 *
 * @param client An instance of the Elasticsearch `Client` used to perform the ping operation.
 * @returns {boolean}
 */
export const ping = async (client: Client): Promise<boolean> => {
	const response = await client.ping();
	return response.body;
};
