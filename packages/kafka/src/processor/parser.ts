import type { DataRecordValue } from '@overture-stack/maestro-common';

/**
 * Parses the Kafka message into a JSON format and returns it.
 * Throws an error if is not valid JSON.
 * @param messageValue
 * @returns
 */
export const parseMessage = (messageValue: Buffer | null): Record<string, DataRecordValue> | null => {
	if (!messageValue) return null;
	try {
		return JSON.parse(messageValue.toString());
	} catch {
		console.error('Invalid JSON message');
		return null;
	}
};
