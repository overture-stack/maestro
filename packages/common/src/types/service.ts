import type { IndexData, IndexResult } from './dataRecord';

export interface IElasticsearchService {
	createIndex(index: string): Promise<boolean>;
	indexData(index: string, data: IndexData): Promise<IndexResult>;
	ping(): Promise<boolean>;
	updateData(index: string, data: IndexData): Promise<IndexResult>;
	deleteData(index: string, id: string): Promise<IndexResult>;
}
