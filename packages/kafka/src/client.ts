import { Kafka } from 'kafkajs';

import type { KafkaConfig } from '@overture-stack/maestro-common';

let kafka: Kafka;

export const client = (config: KafkaConfig) => {
	if (config.servers && !kafka) {
		kafka = new Kafka({
			brokers: config.servers?.split(','),
		});
	}
	return kafka;
};
