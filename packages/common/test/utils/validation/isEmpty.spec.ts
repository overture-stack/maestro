import { expect } from 'chai';
import { describe, it } from 'mocha';

import isEmpty from '../../../src/utils/validation/isEmpty.js';

describe('Validation functions', () => {
	describe('isEmpty', () => {
		it('should return true when argument is `null`', () => {
			const result = isEmpty(null);
			expect(result).to.eql(true);
		});
		it('should return true when argument is `undefined`', () => {
			const result = isEmpty(undefined);
			expect(result).to.eql(true);
		});
		it('should return true when argument is an empty string', () => {
			const result = isEmpty('');
			expect(result).to.eql(true);
		});
		it('should return true when argument is an empty array', () => {
			const result = isEmpty([]);
			expect(result).to.eql(true);
		});
		it('should return true when argument is an empty object', () => {
			const result = isEmpty({});
			expect(result).to.eql(true);
		});
		it('should return false when argument is not empty string', () => {
			const result = isEmpty('abc');
			expect(result).to.eql(false);
		});
		it('should return false when argument is a number', () => {
			const result = isEmpty(123);
			expect(result).to.eql(false);
		});
		it('should return false when argument is an object', () => {
			const result = isEmpty({ tile: 'ABC' });
			expect(result).to.eql(false);
		});
	});
});
