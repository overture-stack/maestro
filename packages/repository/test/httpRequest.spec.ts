import { expect } from 'chai';
import fetchMock from 'fetch-mock';
import { describe, it } from 'mocha';

import { sendHttpRequest } from '../src/network/httpRequest';

describe('sendHttpRequest', () => {
	beforeEach(() => {
		fetchMock.mockGlobal();
	});

	afterEach(() => {
		fetchMock.hardReset();
	});

	it('should make a successful GET request and return response', async () => {
		const url = 'https://api.example.com/data';

		fetchMock.get(url, 200);

		const result = await sendHttpRequest(url);
		expect(fetchMock.callHistory.calls().length).to.eql(1);
		expect(result.ok).to.eql(true);
	});

	it('should make a GET request and return 500 error code', async () => {
		const url = 'https://api.example.com/data';

		fetchMock.get(url, 500);

		const result = await sendHttpRequest(url);
		expect(fetchMock.callHistory.calls().length).to.eql(1);
		expect(result.ok).to.eql(false);
	});

	it('should make a GET request and throw an Error', async () => {
		const url = 'https://api.example.com/data';

		fetchMock.get(url, { throws: { message: 'Network error' } });

		try {
			await sendHttpRequest(url);
		} catch (error) {
			expect(fetchMock.callHistory.calls().length).to.eql(1);
			expect(error.message).to.eql('Network error');
		}
	});
});
