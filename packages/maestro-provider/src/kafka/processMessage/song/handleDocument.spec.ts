import { expect } from 'chai';
import { describe, it } from 'mocha';

import {
	type DataRecordNested,
	IndexableState,
	IndexingMode,
	RepositoryType,
	type SongRepositoryConfig,
} from '@overture-stack/maestro-common';

import { createRecordingIndexer } from '../../../../testing/peripherals/recordingIndexer.js';
import { handleSongDocumentMessage } from './handleDocument.js';

const songConfig: SongRepositoryConfig = {
	baseUrl: 'http://song.test',
	code: 'song-repo',
	indexableStudyStates: [IndexableState.PUBLISHED],
	indexingMode: IndexingMode.analysisCentric,
	indexName: 'song-index',
	name: 'Song repository',
	type: RepositoryType.SONG,
};

const unpublishedAnalysisMessage: DataRecordNested = {
	analysis: {
		analysisId: 'AN-1',
		analysisState: 'UNPUBLISHED',
		files: [{ fileName: 'TEST-CA.fasta', objectId: 'OBJ-1' }],
		studyId: 'TEST-CA',
	},
	analysisId: 'AN-1',
	songServerId: 'collab',
	state: 'UNPUBLISHED',
	studyId: 'TEST-CA',
};

describe('handleSongDocumentMessage', () => {
	describe('for an analysis outside the indexable states', () => {
		it('removes the document with the analysis ID', async () => {
			const { deletedIds, indexer } = createRecordingIndexer([]);

			await handleSongDocumentMessage(songConfig, unpublishedAnalysisMessage, indexer);

			expect(deletedIds).to.eql(['AN-1']);
		});
	});
});
