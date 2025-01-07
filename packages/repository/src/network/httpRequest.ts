import { logger } from '@overture-stack/maestro-common';

interface FetchOptions extends RequestInit {
	headers?: Record<string, string>;
}

const defaultHeaders: Record<string, string> = {
	'Content-Type': 'application/json',
	Accept: 'application/json',
};

export const sendHttpRequest = async (url: string, options: FetchOptions = {}) => {
	const config: FetchOptions = {
		...options,
		headers: {
			...defaultHeaders,
			...(options.headers || {}),
		},
	};

	try {
		logger.debug(`${config.method ?? 'GET'} ${url}`);
		return await fetch(url, config);
	} catch (error) {
		logger.error('Fetch error', error);
		throw error;
	}
};
