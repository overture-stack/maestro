interface FetchOptions extends RequestInit {
	headers?: Record<string, string>;
}

const defaultHeaders: Record<string, string> = {
	'Content-Type': 'application/json',
	Accept: 'application/json',
};

const httpClient = async (url: string, options: FetchOptions = {}) => {
	const config: FetchOptions = {
		...options,
		headers: {
			...defaultHeaders,
			...(options.headers || {}),
		},
	};

	try {
		console.debug(`${config.method ?? 'GET'} ${url}`);
		const response = await fetch(url, config);
		if (!response.ok) {
			throw new Error(`HTTP error! Status: ${response.status}`);
		}
		return response.json();
	} catch (error) {
		console.error('Fetch error', error);
		throw error;
	}
};

export default httpClient;
