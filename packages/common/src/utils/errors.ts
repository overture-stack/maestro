const todaysDate = () => {
	return new Date().toISOString();
};

/**
 * A request Maestro cannot carry out as given. Maestro's server answers it with `400`, sending `details` in the response
 * body.
 */
export class BadRequest extends Error {
	details?: string;
	timestamp: string;

	constructor(details?: string) {
		super('Bad Request');
		this.details = details;
		this.timestamp = todaysDate();
	}
}

/**
 * A request naming something that does not exist. Maestro's server answers it with `404`, sending `details` in the
 * response body.
 */
export class NotFound extends Error {
	details?: string;
	timestamp: string;
	constructor(details?: string) {
		super('Not Found');
		this.details = details;
		this.timestamp = todaysDate();
	}
}

/**
 * A request for something Maestro does not do yet. Maestro's server answers it with `501`, sending `details` in the
 * response body.
 */
export class NotImplemented extends Error {
	details?: string;
	timestamp: string;
	constructor(details?: string) {
		super('Not Implemented');
		this.details = details || 'This functionallity is not yet implemented';
		this.timestamp = todaysDate();
	}
}

/**
 * A request Maestro cannot carry out because a resource it needs cannot be reached. Maestro's server answers it with
 * `503`, sending `details` in the response body.
 */
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

/**
 * Any other failure on Maestro's side, or in a service it depends on. Maestro's server answers it with `500`, sending
 * `details` in the response body.
 */
export class InternalServerError extends Error {
	details?: string;
	timestamp: string;
	constructor(details?: string) {
		super('Internal Server Error');
		this.details = details || 'Something unexpected happened';
		this.timestamp = todaysDate();
	}
}

/**
 * The message of `error` when it is an `Error`, and `error` as a string otherwise.
 */
export const getErrorMessage = (error: unknown) => {
	if (error instanceof Error) return error.message;
	return String(error);
};
