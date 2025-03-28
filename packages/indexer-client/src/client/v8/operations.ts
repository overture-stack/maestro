import type { Client, estypes } from 'es8';

import { type DataRecordNested, FailureData, IndexResult, logger } from '@overture-stack/maestro-common';

export const bulkUpsert = async (client: Client, index: string, dataSet: DataRecordNested[]) => {
	try {
		const body = dataSet.flatMap(({ _id, ...doc }) => [{ index: { _index: index, _id } }, doc]);

		const response = await client.bulk({ refresh: true, body });

		logger.info(`Bulk upsert in index:'${index}'`, `# of documents: ${response.items.length}`);

		const failureData: FailureData = {};

		if (response.errors) {
			// The items array has the same order of the dataset we just indexed.
			// The presence of the `error` key indicates that the operation
			// that we did for the document has failed.
			response.items.forEach(
				(item: Partial<Record<estypes.BulkOperationType, estypes.BulkResponseItem>>, indexItem: number) => {
					const operation = item.index;
					if (operation && 'error' in operation) {
						failureData[indexItem] = [operation.error?.reason || 'error'];
					}
				},
			);
		}

		return {
			indexName: index,
			successful: !Object.keys(failureData).length,
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
		exists = await client.indices.exists({ index });
		if (!exists) {
			await client.indices.create({ index });
			logger.info(`Index ${index} created.`);
		} else {
			logger.info(`Index ${index} already exists.`);
		}
	} catch (error) {
		logger.error(`Error creating the index: ${error}`);
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
			document: data,
		});

		logger.info(`Indexing document in:'${index}'`);

		let successful = false;
		const failureData: FailureData = {};
		if (response.result === 'created' || response.result === 'updated') {
			successful = true;
		} else {
			const keyIndex = Object.keys(failureData).length;
			failureData[keyIndex] = [response.result];
		}
		return {
			indexName: response._index,
			successful,
			failureData,
		};
	} catch (error) {
		logger.error(`Error document index: ${error}`);

		let errorMessage = JSON.stringify(error);
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
		logger.info(`Updating indexed document in:'${index}'`);

		let successful = false;
		const failureData: FailureData = {};
		if (response.result === 'created' || response.result === 'updated') {
			successful = true;
		} else {
			failureData[id] = [response.result];
		}

		return {
			indexName: response._index,
			successful,
			failureData,
		};
	} catch (error) {
		logger.error(`Error updating the index: ${error}`);

		let errorMessage = JSON.stringify(error);
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
		logger.info(`Deleting indexed document in:'${index}'`);

		let successful = false;
		const failureData: FailureData = {};

		if (response.result === 'deleted') {
			successful = true;
		} else {
			failureData[id] = [response.result];
		}
		return {
			indexName: response._index,
			successful,
			failureData,
		};
	} catch (error) {
		logger.error(`Error deleting the index: ${error}`);

		let errorMessage = JSON.stringify(error);
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
		return await client.ping();
	} catch (error) {
		logger.error(`Error ping server: ${error}`);

		return false;
	}
};
