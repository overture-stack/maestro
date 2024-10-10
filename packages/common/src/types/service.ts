import type { DataRecordValue, IndexData, IndexResult } from './dataRecord.js';

export interface IElasticsearchService {
	createIndex(index: string): Promise<boolean>;
	indexData(index: string, data: IndexData): Promise<IndexResult>;
	ping(): Promise<boolean>;
	updateData(index: string, id: string, data: Record<string, DataRecordValue>): Promise<IndexResult>;
	deleteData(index: string, id: string): Promise<IndexResult>;
}
