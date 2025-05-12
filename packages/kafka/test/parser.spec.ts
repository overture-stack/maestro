import { expect } from 'chai';

import { parseMessage } from '../src/processMessage/parser';

describe('parse kafka message', () => {
	it('should return null for null input', () => {
		const result = parseMessage(null);
		expect(result).to.eql(null);
	});

	it('should parse valid JSON buffer correctly', () => {
		const json = { foo: 'bar', count: 42 };
		const buffer = Buffer.from(JSON.stringify(json));

		const result = parseMessage(buffer);
		expect(result).to.eql(json);
	});

	it('should return null for invalid JSON and log an error', () => {
		const invalidJson = Buffer.from('{ invalid: json }');

		try {
			parseMessage(invalidJson);
		} catch (error) {
			expect(error.message).to.eql('Invalid JSON message');
		}
	});
});
