import { expect } from 'chai';

import { isFileEntry, isFileEntryArray } from '../../../src/utils/analysisConverter/converter';

describe('File object functions', () => {
	const validFileEntry = {
		objectId: '123',
		studyId: 'STUDY-1',
		analysisId: 'ANALYSIS-1',
		fileName: 'file.txt',
		fileSize: '1024',
		fileType: 'txt',
		fileMd5sum: 'abcd1234',
		fileAccess: 'controlled',
		dataType: 'clinical',
	};
	describe('isFileEntry', () => {
		it('should return true for a valid FileEntry object', () => {
			expect(isFileEntry(validFileEntry)).to.eql(true);
		});

		it('should return false if one property is missing', () => {
			const invalidFileEntry: Partial<typeof validFileEntry> = { ...validFileEntry };
			delete invalidFileEntry.fileName;

			expect(isFileEntry(invalidFileEntry)).to.eql(false);
		});

		it('should return false for null', () => {
			expect(isFileEntry(null)).to.eql(false);
		});

		it('should return false for non-object values', () => {
			expect(isFileEntry(42)).to.eql(false);
			expect(isFileEntry('string')).to.eql(false);
			expect(isFileEntry(undefined)).to.eql(false);
		});
	});

	describe('isFileEntryArray', () => {
		it('should return true for an array of valid FileEntry objects', () => {
			expect(isFileEntryArray([validFileEntry, validFileEntry])).to.eql(true);
		});

		it('should return false if any element in the array is invalid', () => {
			const invalidFileEntry: Partial<typeof validFileEntry> = { ...validFileEntry };
			delete invalidFileEntry.fileName;

			expect(isFileEntryArray([validFileEntry, invalidFileEntry])).to.eql(false);
		});

		it('should return false for non-array values', () => {
			expect(isFileEntryArray(validFileEntry)).to.eql(false);
			expect(isFileEntryArray('not-an-array')).to.eql(false);
			expect(isFileEntryArray(null)).to.eql(false);
		});

		it('should return false for an empty array', () => {
			expect(isFileEntryArray([])).to.eql(false);
		});
	});
});
