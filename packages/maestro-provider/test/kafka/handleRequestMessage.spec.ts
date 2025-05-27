import { expect } from 'chai';

import { isAnalysisRequest, isRepoRequest, isStudyRequest } from '../../src/kafka/processMessage/requestMessage';

describe('Song Request Message Type Guards', () => {
	describe('isAnalysisRequest', () => {
		it('should return true for a valid analysis request', () => {
			const message = {
				analysisId: 'a-123',
				studyId: 's-456',
				repositoryCode: 'repoA',
			};
			expect(isAnalysisRequest(message)).to.eql(true);
		});

		it('should return false if any required field is missing', () => {
			expect(isAnalysisRequest({ studyId: 's-456', repositoryCode: 'repoA' })).to.eql(false);
			expect(isAnalysisRequest({ analysisId: 'a-123', repositoryCode: 'repoA' })).to.eql(false);
			expect(isAnalysisRequest({ analysisId: 'a-123', studyId: 's-456' })).to.eql(false);
		});
	});

	describe('isStudyRequest', () => {
		it('should return true for a valid study request', () => {
			const message = {
				studyId: 's-456',
				repositoryCode: 'repoA',
			};
			expect(isStudyRequest(message)).to.eql(true);
		});

		it('should return false if analysisId is present', () => {
			const message = {
				analysisId: 'a-123',
				studyId: 's-456',
				repositoryCode: 'repoA',
			};
			expect(isStudyRequest(message)).to.eql(false);
		});

		it('should return false if required fields are missing', () => {
			expect(isStudyRequest({ repositoryCode: 'repoA' })).to.eql(false);
			expect(isStudyRequest({ studyId: 's-456' })).to.eql(false);
		});
	});

	describe('isRepoRequest', () => {
		it('should return true for a valid repository request', () => {
			const message = {
				repositoryCode: 'repoA',
			};
			expect(isRepoRequest(message)).to.eql(true);
		});

		it('should return false if analysisId or studyId is present', () => {
			expect(isRepoRequest({ repositoryCode: 'repoA', analysisId: 'a-123' })).to.eql(false);
			expect(isRepoRequest({ repositoryCode: 'repoA', studyId: 's-456' })).to.eql(false);
		});

		it('should return false if repositoryCode is missing', () => {
			expect(isRepoRequest({})).to.eql(false);
		});
	});
});
