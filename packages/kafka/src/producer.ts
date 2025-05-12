import type { KafkaMessage, Producer } from 'kafkajs';

import { logger } from '@overture-stack/maestro-common';

const DEFAULT_DLQ_TOPIC = 'default-dlq';

/**
 * Sends a Kafka message to a Dead Letter Queue (DLQ) topic
 * @param producer - An instance of a Kafka `Producer` used to send the message
 * @param message - The Kafka message to be sent to the DLQ
 * @param repoDlqTopic - (Optional) A custom DLQ topic to override the default one
 */
export const sendToDLQ = async (producer: Producer, message: KafkaMessage, repoDlqTopic?: string) => {
	const topic = repoDlqTopic || DEFAULT_DLQ_TOPIC;
	try {
		await producer.send({
			topic,
			messages: [message],
		});
		logger.info(`Message sent to DLQ: ${topic}`);
	} catch (error) {
		logger.error('Failed to send message to DLQ', { error });
	}
};
