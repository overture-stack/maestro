import { Kafka } from 'kafkajs';

import { type KafkaConfig, logger } from '@overture-stack/maestro-common';

let kafka: Kafka;

export const client = async (config: KafkaConfig) => {
	if (config.brokers && !kafka) {
		logger.info(`initializing Kafka client with brokers: ${config.brokers?.split(',')}`);
		kafka = new Kafka({
			brokers: config.brokers?.split(','),
		});
	}
	return kafka;
};
