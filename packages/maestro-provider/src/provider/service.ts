import type { AppConfig } from 'src/config/config.js';

import { type DataRecordValue, IElasticsearchService, type IndexData } from '@overture-stack/maestro-common';
import { es7, es8 } from '@overture-stack/maestro-indexer-client';

export type ESVersion = '7' | '8';

export const clientProvider = (appConfig: AppConfig): IElasticsearchService => {
	let service: IElasticsearchService;

	const elasticSearchConfig = appConfig.elasticSearchConfig;

	if (elasticSearchConfig.version === 7) {
		service = es7(elasticSearchConfig);
	} else if (elasticSearchConfig.version === 8) {
		service = es8(elasticSearchConfig);
	} else {
		throw new Error('Unsupported Elasticsearch version');
	}

	return {
		addData: (index: string, data: IndexData) => service.addData(index, data),
		createIndex: (index: string) => service.createIndex(index),
		deleteData: (index: string, id: string) => service.deleteData(index, id),
		ping: () => service.ping(),
		updateData: (index: string, id: string, data: Record<string, DataRecordValue>) =>
			service.updateData(index, id, data),
	};
};
