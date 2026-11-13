import { expect } from 'chai';
import { describe, it } from 'mocha';

import { buildScanQuery, isDocumentNotFound, toDocumentFieldValues } from './documents.js';

describe('buildScanQuery', () => {
	it('matches every document when no field value is given', () => {
		expect(buildScanQuery('organization')).to.eql({ match_all: {} });
	});

	it('matches the documents holding exactly the field value when one is given', () => {
		expect(buildScanQuery('organization', 'Lab A')).to.eql({ term: { organization: 'Lab A' } });
	});
});

describe('toDocumentFieldValues', () => {
	const hits = [
		{ _id: 'exact', _source: { organization: 'abc' } },
		{ _id: 'upper', _source: { organization: 'ABC' } },
		{ _id: 'words', _source: { organization: 'abc lab' } },
		{ _id: 'none', _source: {} },
	];

	it('pairs every document with its field value when no field value is given', () => {
		expect(toDocumentFieldValues(hits, 'organization')).to.eql([
			{ documentId: 'exact', fieldValue: 'abc' },
			{ documentId: 'upper', fieldValue: 'ABC' },
			{ documentId: 'words', fieldValue: 'abc lab' },
			{ documentId: 'none', fieldValue: undefined },
		]);
	});

	it('keeps only documents holding the field value exactly, case included', () => {
		expect(toDocumentFieldValues(hits, 'organization', 'abc')).to.eql([{ documentId: 'exact', fieldValue: 'abc' }]);
	});

	it('skips a hit without a document ID', () => {
		expect(toDocumentFieldValues([{ _source: { organization: 'abc' } }], 'organization')).to.eql([]);
	});
});

describe('isDocumentNotFound', () => {
	it("recognizes Elasticsearch's answer to deleting a document missing from the index", () => {
		expect(isDocumentNotFound({ meta: { body: { result: 'not_found' }, statusCode: 404 } })).to.equal(true);
	});

	it('does not match the answer for an index that does not exist', () => {
		const missingIndex = { meta: { body: { error: { type: 'index_not_found_exception' } }, statusCode: 404 } };

		expect(isDocumentNotFound(missingIndex)).to.equal(false);
	});

	it('does not match another failure, or a value that is not an Elasticsearch error', () => {
		expect(isDocumentNotFound({ meta: { body: { result: 'not_found' }, statusCode: 500 } })).to.equal(false);
		expect(isDocumentNotFound(new Error('connection refused'))).to.equal(false);
		expect(isDocumentNotFound(undefined)).to.equal(false);
	});
});
