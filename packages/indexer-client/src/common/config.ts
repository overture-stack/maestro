import type { ElasticSearchBasicAuthConfig } from '@overture-stack/maestro-common';

export const getAuth = (basicAuthConfig: ElasticSearchBasicAuthConfig) => {
	return basicAuthConfig.enabled
		? {
				username: basicAuthConfig.user ?? '',
				password: basicAuthConfig.password ?? '',
			}
		: undefined;
};
