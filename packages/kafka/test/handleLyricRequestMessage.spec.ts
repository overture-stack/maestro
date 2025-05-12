import { expect } from 'chai';

import { isDocumentRequest, isOrganizationRequest, isRepositoryRequest } from '../src/processor/lyric/handleRequest';

describe('Lyric Request Message Type Guards', () => {
	describe('isDocumentRequest', () => {
		it('should return true for a valid document request', () => {
			const message = {
				systemId: '123',
				organization: 'org1',
				repositoryCode: 'repoA',
			};
			expect(isDocumentRequest(message)).to.eql(true);
		});

		it('should return false if any required field is missing', () => {
			const incompleteMessage1 = {
				organization: 'org1',
				repositoryCode: 'repoA',
			};
			const incompleteMessage2 = {
				systemId: '123',
				repositoryCode: 'repoA',
			};
			const incompleteMessage3 = {
				systemId: '123',
				organization: 'org1',
			};
			expect(isDocumentRequest(incompleteMessage1)).to.eql(false);
			expect(isDocumentRequest(incompleteMessage2)).to.eql(false);
			expect(isDocumentRequest(incompleteMessage3)).to.eql(false);
		});
	});

	describe('isOrganizationRequest', () => {
		it('should return true for a valid organization request', () => {
			const message = {
				organization: 'org1',
				repositoryCode: 'repoA',
			};
			expect(isOrganizationRequest(message)).to.eql(true);
		});

		it('should return false if systemId is present', () => {
			const message = {
				systemId: '123',
				organization: 'org1',
				repositoryCode: 'repoA',
			};
			expect(isOrganizationRequest(message)).to.eql(false);
		});

		it('should return false if any required field is missing', () => {
			expect(isOrganizationRequest({ repositoryCode: 'repoA' })).to.eql(false);
		});
	});

	describe('isRepositoryRequest', () => {
		it('should return true for a valid repository request', () => {
			const message = {
				repositoryCode: 'repoA',
			};
			expect(isRepositoryRequest(message)).to.eql(true);
		});

		it('should return false if systemId or organization is present', () => {
			expect(isRepositoryRequest({ repositoryCode: 'repoA', systemId: '123' })).to.eql(false);
			expect(isRepositoryRequest({ repositoryCode: 'repoA', organization: 'org1' })).to.eql(false);
		});

		it('should return false if repositoryCode is missing', () => {
			expect(isRepositoryRequest({})).to.eql(false);
		});
	});
});
