const todaysDate = () => {
	return new Date().toISOString();
};

export class BadRequest extends Error {
	details?: string;
	timestamp: string;

	constructor(details?: string) {
		super('Bad Request');
		this.details = details;
		this.timestamp = todaysDate();
	}
}

export class NotFound extends Error {
	details?: string;
	timestamp: string;
	constructor(details?: string) {
		super('Not Found');
		this.details = details;
		this.timestamp = todaysDate();
	}
}

export class NotImplemented extends Error {
	details?: string;
	timestamp: string;
	constructor(details?: string) {
		super('Not Implemented');
		this.details = details || 'This functionallity is not yet implemented';
		this.timestamp = todaysDate();
	}
}

export class ServiceUnavailable extends Error {
	details?: string;
	timestamp: string;
	constructor(details?: string) {
		super('Service unavailable');
		this.details =
			details || 'Server is unable to access the necessary resources to process the request. Please try again later.';
		this.timestamp = todaysDate();
	}
}

export class InternalServerError extends Error {
	details?: string;
	timestamp: string;
	constructor(details?: string) {
		super('Internal Server Error');
		this.details = details || 'Something unexpected happened';
		this.timestamp = todaysDate();
	}
}

export const getErrorMessage = (error: unknown) => {
	if (error instanceof Error) return error.message;
	return String(error);
};
