import { expect } from 'chai';

import type { DataRecordNested } from '../../../src/types';
import { convertToAnalysisCentricDocuments } from '../../../src/utils/analysisConverter/converter';

describe('convertToAnalysisCentricDocuments', () => {
	it('should convert records into analysis-centric documents', () => {
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
				files: [],
			},
		];

		const result = convertToAnalysisCentricDocuments(records);
		expect(result).to.have.lengthOf(1);
		expect(result[0]).to.eql({
			_id: 'A1',
			analysisId: 'A1',
			studyId: 'S1',
			state: 'PUBLISHED',
			analysis: {
				analysisId: 'A1',
				studyId: 'S1',
				analysisState: 'PUBLISHED',
				createdAt: '2025-05-14',
				experiment: {
					purpose_of_sequencing_details: 'Not Provided',
				},
				donors: [],
				files: [],
			},
		});
		expect(result[0].analysis).to.eql(records[0]);
	});
});
