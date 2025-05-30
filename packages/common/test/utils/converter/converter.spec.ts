import { expect } from 'chai';

import {
	type DataRecordNested,
	IndexableState,
	IndexingMode,
	RepositoryType,
	type SongRepositoryConfig,
} from '../../../src/types';
import { convertAnalyses } from '../../../src/utils/analysisConverter/converter';
import type { FileEntry } from '../../../src/utils/analysisConverter/types';

describe('convertAnalyses', () => {
	const repoFileCentric: SongRepositoryConfig = {
		code: 'REPO1',
		organization: 'ORG1',
		name: 'Repository One',
		type: RepositoryType.SONG,
		country: 'US',
		baseUrl: 'http://localhost',
		indexableStudyStates: [IndexableState.PUBLISHED],
		indexName: 'my-index',
		indexingMode: IndexingMode.fileCentric,
	};

	const repoAnalysisCentric: SongRepositoryConfig = {
		...repoFileCentric,
		indexingMode: IndexingMode.analysisCentric,
	};

	const file: FileEntry = {
		objectId: 'f001',
		studyId: 'S1',
		fileName: 'file.bam',
		fileSize: 123456,
		fileType: 'bam',
		fileMd5sum: 'abc123',
		fileAccess: 'controlled',
		dataType: 'genomic',
	};

	const record: DataRecordNested = {
		analysisId: 'A1',
		studyId: 'S1',
		analysisState: 'PUBLISHED',
		files: [file],
	};

	it('should use file-centric conversion when indexingMode is fileCentric', () => {
		const result = convertAnalyses(repoFileCentric, [record]);
		expect(result).to.have.lengthOf(1);
		expect(result[0]).to.have.property('_id', 'f001');
		expect(result[0]).to.have.property('object_id', 'f001');
	});

	it('should use analysis-centric conversion when indexingMode is analysisCentric', () => {
		const result = convertAnalyses(repoAnalysisCentric, [record]);
		expect(result).to.have.lengthOf(1);
		expect(result[0]).to.have.property('_id', 'A1');
		expect(result[0]).to.have.property('analysisId', 'A1');
	});
});
