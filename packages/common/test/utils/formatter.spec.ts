import { expect } from 'chai';
import { describe, it } from 'mocha';

import { sanitize_index_name } from '../../src/utils/index';

describe('Formatter functions', () => {
	describe('Sanitize string', () => {
		it('should replace empty spaces in a string', () => {
			const myString = 'a b c';
			const result = sanitize_index_name(myString);
			expect(result).to.eql('a_b_c');
		});
		it('should replace less than character in a string', () => {
			const myString = 'a<b<c';
			const result = sanitize_index_name(myString);
			expect(result).to.eql('a_b_c');
		});
		it('should replace greater than character in a string', () => {
			const myString = 'a>b>c';
			const result = sanitize_index_name(myString);
			expect(result).to.eql('a_b_c');
		});
		it('should replace double quotes in a string', () => {
			const myString = 'a"b"c';
			const result = sanitize_index_name(myString);
			expect(result).to.eql('a_b_c');
		});
		it('should replace back slash in a string', () => {
			const myString = 'a\\b\\c';
			const result = sanitize_index_name(myString);
			expect(result).to.eql('a_b_c');
		});
		it('should replace forward slash in a string', () => {
			const myString = 'a/b/c';
			const result = sanitize_index_name(myString);
			expect(result).to.eql('a_b_c');
		});
		it('should replace comma in a string', () => {
			const myString = 'a,b,c';
			const result = sanitize_index_name(myString);
			expect(result).to.eql('a_b_c');
		});
		it('should replace pipe in a string', () => {
			const myString = 'a|b|c';
			const result = sanitize_index_name(myString);
			expect(result).to.eql('a_b_c');
		});
		it('should replace question mark in a string', () => {
			const myString = 'a?b?c';
			const result = sanitize_index_name(myString);
			expect(result).to.eql('a_b_c');
		});
		it('should replace asterisk in a string', () => {
			const myString = 'a*b*c';
			const result = sanitize_index_name(myString);
			expect(result).to.eql('a_b_c');
		});
		it('should convert string to lower case', () => {
			const myString = 'ABC';
			const result = sanitize_index_name(myString);
			expect(result).to.eql('abc');
		});
	});
});
