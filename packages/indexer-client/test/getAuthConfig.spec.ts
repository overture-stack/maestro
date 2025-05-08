import { expect } from 'chai';

import type { ElasticSearchBasicAuthConfig } from '@overture-stack/maestro-common';

import { getAuth } from '../src/common/config.js';

describe('getAuth', () => {
	it('should return auth credentials if auth is enabled', () => {
		const authConfigEnabled: ElasticSearchBasicAuthConfig = {
			enabled: true,
			user: 'admin',
			password: '123456',
		};
		const auth = getAuth(authConfigEnabled);

		expect(auth).to.eql({ username: 'admin', password: '123456' });
	});

	it('should return empty if auth is enabled and credentials are not set', () => {
		const authConfigEnabled: ElasticSearchBasicAuthConfig = {
			enabled: true,
		};
		const auth = getAuth(authConfigEnabled);

		expect(auth).to.eql({ username: '', password: '' });
	});
	it('should return undefined if auth is disabled', () => {
		const authConfigDisabled: ElasticSearchBasicAuthConfig = {
			enabled: false,
		};
		const auth = getAuth(authConfigDisabled);

		expect(auth).to.eql(undefined);
	});
});
