import type { Client } from 'es7';
import type { BulkOperationType, BulkResponseItem } from 'es7/api/types';

import { type DataRecordNested, type FailureData, IndexResult, logger } from '@overture-stack/maestro-common';

/**
 * Indexes the specified document. If the document exists, replaces the document and increments the version.
 *
 * @param client An instance of the Elasticsearch `Client` used to perform the indexing operation
 * @param index The name of the Elasticsearch index to create
 * @param dataSet The actual data to be stored in the document
 * @returns
 */
export const bulkUpsert = async (client: Client, index: string, dataSet: DataRecordNested[]) => {
	try {
		const body = dataSet.flatMap((doc) => [{ index: { _index: index, _id: doc?.['id'] } }, doc]);

		const response = await client.bulk({ refresh: true, body });

		logger.debug(`Bulk upsert in index:'${index}'`, `# of documents:'${dataSet.length}'`, response.statusCode);

		let successful = false;
		const failureData: FailureData = {};
		if (response.body.errors) {
			// The items array has the same order of the dataset we just indexed.
			// The presence of the `error` key indicates that the operation
			// that we did for the document has failed.
			response.body.items.forEach((item: Partial<Record<BulkOperationType, BulkResponseItem>>, indexItem: number) => {
				const operation = item.index;
				if (operation && 'error' in operation) {
					failureData[indexItem] = [operation.error?.reason || 'error'];
				}
			});
		}

		if (Object.keys(failureData).length === 0) {
			successful = true;
		}

		return {
			indexName: index,
			successful,
			failureData,
		};
	} catch (error) {
		let errorMessage = JSON.stringify(error);

		logger.error(`Error update doc: ${errorMessage}`);

		if (typeof error === 'object' && error && 'name' in error && typeof error.name === 'string') {
			errorMessage = error.name;
		}
		return {
			indexName: index,
			successful: false,
			failureData: { error: [errorMessage] },
		};
	}
};

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
	let exists = false;
	try {
		const result = await client.indices.exists({ index });
		exists = result.body;
		if (!result.body) {
			await client.indices.create({ index });
			logger.info(`Index ${index} created.`);
		} else {
			logger.debug(`Index ${index} already exists.`);
		}
	} catch (error) {
		logger.error(`Error creating the index: ${JSON.stringify(error)}`);
	}
	return exists;
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
export const indexData = async (client: Client, index: string, data: DataRecordNested): Promise<IndexResult> => {
	try {
		const response = await client.index({
			index,
			id: data?.['id']?.toString(),
			body: data,
		});
		logger.debug(`Indexing document in:'${index}'`, response.statusCode);

		let successful = false;
		const failureData: FailureData = {};
		if (response.body.result === 'created' || response.body.result === 'updated') {
			successful = true;
		} else {
			const keyIndex = Object.keys(failureData).length;
			failureData[keyIndex] = [response.body.result];
		}

		return {
			indexName: index,
			successful,
			failureData,
		};
	} catch (error) {
		let errorMessage = JSON.stringify(error);

		logger.error(`Error index doc: ${errorMessage}`);

		if (typeof error === 'object' && error && 'name' in error && typeof error.name === 'string') {
			errorMessage = error.name;
		}
		return {
			indexName: index,
			successful: false,
			failureData: { [data?.['id']?.toString() || 0]: [errorMessage] },
		};
	}
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
	id: string,
	data: DataRecordNested,
): Promise<IndexResult> => {
	try {
		const response = await client.update({
			index,
			id,
			body: {
				doc: { data },
			},
		});
		logger.debug(`Updating indexed document in:'${index}'`, response.statusCode);

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
	} catch (error) {
		let errorMessage = JSON.stringify(error);

		logger.error(`Error update doc: ${errorMessage}`);

		if (typeof error === 'object' && error && 'name' in error && typeof error.name === 'string') {
			errorMessage = error.name;
		}
		return {
			indexName: index,
			successful: false,
			failureData: { [id]: [errorMessage] },
		};
	}
};

/**
 * Deletes a document from a specified Elasticsearch index
 * @param client An instance of the Elasticsearch `Client` used to perform the delete operation
 * @param index The name of the Elasticsearch index from which the document will be deleted
 * @param id The unique identifier of the document to be deleted
 * @returns A promise that resolves to a `IndexResult`, containing metadata about the operation result
 */
export const deleteData = async (client: Client, index: string, id: string): Promise<IndexResult> => {
	try {
		const response = await client.delete({
			index,
			id,
		});
		logger.debug(`Deleting indexed document in:'${index}'`, response.statusCode);

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
	} catch (error) {
		let errorMessage = JSON.stringify(error);

		logger.error(`Error delete doc: ${errorMessage}`);

		if (typeof error === 'object' && error && 'name' in error && typeof error.name === 'string') {
			errorMessage = error.name;
		}
		return {
			indexName: index,
			successful: false,
			failureData: { [id]: [errorMessage] },
		};
	}
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
	try {
		const response = await client.ping();
		return response.body;
	} catch (error) {
		logger.error(`Error ping server: ${JSON.stringify(error)}`);

		return false;
	}
};
