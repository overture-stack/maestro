import {
	convertAnalyses,
	type DataRecordNested,
	DOCUMENT_NOT_FOUND,
	type DocumentFieldValue,
	type ElasticsearchService,
	logger,
	type LyricRepositoryConfig,
	RepositoryType,
	type SongRepositoryConfig,
} from '@overture-stack/maestro-common';

import { type LyricRepository, lyricRepository, RECORD_STATUS } from '../repository/repositories/lyric/repository.js';

const ORGANIZATION_FIELD_NAME = 'organization';

/**
 * What happened to a document whose record might have been deleted: `removed` from the index; `alreadyAbsent`, as the
 * index no longer held it; `kept`, as Lyric did not confirm the deletion; or `failed`, as the index refused to delete it.
 */
export const REMOVAL_OUTCOME = {
	ALREADY_ABSENT: 'alreadyAbsent',
	FAILED: 'failed',
	KEPT: 'kept',
	REMOVED: 'removed',
} as const;

/**
 * One of the values of `REMOVAL_OUTCOME`.
 */
export type RemovalOutcome = (typeof REMOVAL_OUTCOME)[keyof typeof REMOVAL_OUTCOME];

/**
 * Processes and indexes data records based on the repository type.
 *
 * If the repository type is `SONG`, the function converts the provided items into either
 * file-centric or analysis-centric documents (based on the repository's indexing mode),
 * and then bulk upserts them into the configured Elasticsearch index.
 *
 * For `Lyric` repository types, it upserts the provided items without conversion as they
 * are already formatted.
 *
 * @param items - An array of data records to be indexed.
 * @param repoInfo - The configuration object for the repository, which includes the index name and type.
 * @param indexer - An instance of `ElasticsearchService` used for performing indexing operations.
 */
export const indexRepositoryData = ({
	items,
	repoInfo,
	indexer,
}: {
	items: DataRecordNested[];
	repoInfo: SongRepositoryConfig | LyricRepositoryConfig;
	indexer: ElasticsearchService;
}) => {
	if (repoInfo.type === RepositoryType.SONG) {
		// convert Song documents into fileCentric or analysisCentric document
		const converted = convertAnalyses(repoInfo, items);

		if (converted.length > 0) {
			indexer.bulkUpsert(repoInfo.indexName, converted);
		} else {
			logger.error(`Error converting records into '${repoInfo.indexingMode}Centric document'`);
		}
	} else {
		// Lyric repository type, upsert items directly
		indexer.bulkUpsert(repoInfo.indexName, items);
	}
};

/**
 * Removes the document `documentId` from `indexName` when `lyric` confirms its record deleted from `organization`, and
 * keeps it otherwise.
 *
 * @param documentId - The ID of the document, which is its record's `systemId`.
 * @param indexer - The `ElasticsearchService` holding the index.
 * @param indexName - The index holding the document.
 * @param lyric - The Lyric repository the record belonged to.
 * @param organization - The organization the record belonged to.
 */
export const removeDocumentIfRecordDeleted = async ({
	documentId,
	indexer,
	indexName,
	lyric,
	organization,
}: {
	documentId: string;
	indexer: ElasticsearchService;
	indexName: string;
	lyric: LyricRepository;
	organization: string;
}): Promise<RemovalOutcome> => {
	const status = await lyric.getRecordStatus({ id: documentId, organization });
	if (status === RECORD_STATUS.DELETED) {
		const result = await indexer.deleteData(indexName, documentId);
		if (result.successful) {
			return REMOVAL_OUTCOME.REMOVED;
		}
		return result.failureData[documentId]?.includes(DOCUMENT_NOT_FOUND)
			? REMOVAL_OUTCOME.ALREADY_ABSENT
			: REMOVAL_OUTCOME.FAILED;
	}
	return REMOVAL_OUTCOME.KEPT;
};

const isRemovalCandidate = (document: DocumentFieldValue, indexedIds: Set<string>): boolean =>
	typeof document.fieldValue === 'string' && !indexedIds.has(document.documentId);

const countOutcomes = (outcomes: RemovalOutcome[]): Record<RemovalOutcome, number> =>
	outcomes.reduce<Record<RemovalOutcome, number>>(
		(counts, outcome) => ({ ...counts, [outcome]: counts[outcome] + 1 }),
		{ alreadyAbsent: 0, failed: 0, kept: 0, removed: 0 },
	);

const confirmEachRemoval = async function* ({
	candidates,
	indexer,
	lyricConfig,
}: {
	candidates: DocumentFieldValue[];
	indexer: ElasticsearchService;
	lyricConfig: LyricRepositoryConfig;
}): AsyncGenerator<RemovalOutcome, void, unknown> {
	const lyric = lyricRepository(lyricConfig);
	for (const candidate of candidates) {
		yield await removeDocumentIfRecordDeleted({
			documentId: candidate.documentId,
			indexer,
			indexName: lyricConfig.indexName,
			lyric,
			organization: String(candidate.fieldValue),
		});
	}
};

const removeDeletedDocuments = async ({
	indexedIds,
	indexer,
	lyricConfig,
	organization,
}: {
	indexedIds: Set<string>;
	indexer: ElasticsearchService;
	lyricConfig: LyricRepositoryConfig;
	organization?: string;
}): Promise<void> => {
	const scanDocuments = indexer.scanDocuments?.bind(indexer);
	const logFields = {
		event: 'maestro.reindex.removal',
		indexName: lyricConfig.indexName,
		organization: organization ?? 'all',
	};
	if (scanDocuments) {
		try {
			const scanOptions = {
				fieldName: ORGANIZATION_FIELD_NAME,
				...(organization === undefined ? {} : { fieldValue: organization }),
			};
			// Every candidate is listed before any is confirmed, since confirming them can outlast the index's hold on a
			// listing in progress
			const candidates = (
				await Array.fromAsync(scanDocuments(lyricConfig.indexName, scanOptions), (page) =>
					page.filter((document) => isRemovalCandidate(document, indexedIds)),
				)
			).flat();
			const outcomes = await Array.fromAsync(confirmEachRemoval({ candidates, indexer, lyricConfig }));
			logger.info('Removed the documents of deleted records', { ...logFields, outcomes: countOutcomes(outcomes) });
		} catch (error) {
			const message = error instanceof Error ? error.message : String(error);
			logger.error(`Error removing the documents of deleted records. ${message}`, logFields);
		}
	} else {
		logger.info('Skipped removing the documents of deleted records, as the indexer cannot list documents', logFields);
	}
};

const indexEachPage = async function* ({
	indexer,
	pages,
	repoInfo,
}: {
	indexer: ElasticsearchService;
	pages: AsyncGenerator<DataRecordNested[], void, unknown>;
	repoInfo: LyricRepositoryConfig | SongRepositoryConfig;
}): AsyncGenerator<string[], void, unknown> {
	for await (const items of pages) {
		if (items.length > 0) {
			indexRepositoryData({ indexer, items, repoInfo });
		}
		yield items.map((item) => String(item._id));
	}
};

/**
 * Indexes every page of records yielded by `pages` into the repository's index. For a Lyric repository, once the pass has
 * read every page, it then removes each document missing from the pass, once Lyric confirms its record deleted: the
 * documents of `organization` when it is given, and of every organization otherwise. A pass ending in an error removes
 * nothing, so an unreachable Lyric never empties the index, and neither does an indexer without `scanDocuments`.
 *
 * @param indexer - The `ElasticsearchService` holding the repository's index.
 * @param organization - The organization reindexed, or `undefined` when the whole repository is.
 * @param pages - The repository's records, a page at a time.
 * @param repoInfo - The repository's configuration.
 */
export const reindex = async ({
	indexer,
	organization,
	pages,
	repoInfo,
}: {
	indexer: ElasticsearchService;
	organization?: string;
	pages: AsyncGenerator<DataRecordNested[], void, unknown>;
	repoInfo: LyricRepositoryConfig | SongRepositoryConfig;
}): Promise<void> => {
	const indexedIds = await Array.fromAsync(indexEachPage({ indexer, pages, repoInfo })).then(
		(idPages) => new Set(idPages.flat()),
		(error: unknown) => {
			const message = error instanceof Error ? error.message : String(error);
			logger.error(`Error found indexing repository records. ${message}`);
			return undefined;
		},
	);

	if (indexedIds && repoInfo.type === RepositoryType.LYRIC) {
		await removeDeletedDocuments({ indexedIds, indexer, lyricConfig: repoInfo, organization });
	}
};
