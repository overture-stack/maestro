import { Kafka } from 'kafkajs';

import { type KafkaConfig, logger } from '@overture-stack/maestro-common';

let kafka: Kafka;

export const client = (config: KafkaConfig) => {
	if (config.server && !kafka) {
		logger.info(`initializing Kafka client with brokers: ${config.server?.split(',')}`);
		kafka = new Kafka({
			brokers: config.server?.split(','),
		});
	}
	return kafka;
};
