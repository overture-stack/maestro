import { expect } from 'chai';

import { type DataRecordNested, IndexingMode, RepositoryType, type SongRepositoryConfig } from '../../../src/types';
import { convertToFileCentricDocuments } from '../../../src/utils/analysisConverter/converter';
import type { FileEntry } from '../../../src/utils/analysisConverter/types';

describe('convertToFileCentricDocuments', () => {
	const repoInfo: SongRepositoryConfig = {
		code: 'REPO1',
		organization: 'ORG1',
		name: 'Repository One',
		type: RepositoryType.SONG,
		country: 'CAN',
		baseUrl: 'http://localhost',
		indexableStudyStates: 'PUBLISHED',
		indexName: 'my-index',
		indexingMode: IndexingMode.fileCentric,
	};

	const validFile: FileEntry = {
		objectId: 'f123',
		studyId: 'S1',
		analysisId: 'A1',
		fileName: 'data.txt',
		fileSize: '1234',
		fileType: 'txt',
		fileMd5sum: 'abcd1234',
		fileAccess: 'open',
		dataType: 'clinical',
	};

	it('should convert records into file-centric documents when files are valid', () => {
		const records: DataRecordNested[] = [
			{
				analysisId: 'A1',
				studyId: 'S1',
				analysisState: 'PUBLISHED',
				createdAt: '2025-05-14',
				experiment: {
					purpose_of_sequencing_details: 'Not Provided',
				},
				donors: [],
				files: [validFile],
			},
		];

		const result = convertToFileCentricDocuments(repoInfo, records);
		expect(result).to.have.lengthOf(1);
		const doc = result[0];
		expect(doc._id).to.eql('f123');
		expect(doc.data_type).to.eql('clinical');
		expect(doc.file_access).to.eql('open');
		expect(doc.file_type).to.eql('txt');
		expect(doc.analysis).to.eql({
			analysisId: 'A1',
			analysisState: 'PUBLISHED',
			createdAt: '2025-05-14',
			experiment: {
				purpose_of_sequencing_details: 'Not Provided',
			},
			donors: [],
		});
		expect(doc.file.data_type).to.eql('txt');
		expect(doc.file.md5sum).to.eql('abcd1234');
		expect(doc.file.name).to.eql('data.txt');
		expect(doc.file.size).to.eql('1234');
		expect(doc.object_id).to.eql('f123');
		expect(doc.repositories[0].code).to.eql('REPO1');
		expect(doc.repositories[0].country).to.eql('CAN');
		expect(doc.repositories[0].name).to.eql('Repository One');
		expect(doc.repositories[0].organization).to.eql('ORG1');
		expect(doc.repositories[0].type).to.eql('SONG');
		expect(doc.repositories[0].url).to.eql('http://localhost');
		expect(doc.study_id).to.eql('S1');
	});

	it('should return an empty array if files are missing or invalid', () => {
		const records: DataRecordNested[] = [
			{
				analysisId: 'A2',
				studyId: 'S2',
				analysisState: 'DRAFT',
				files: [], // empty files
			},
		];

		const result = convertToFileCentricDocuments(repoInfo, records);
		expect(result).to.eql([]);
	});
});
