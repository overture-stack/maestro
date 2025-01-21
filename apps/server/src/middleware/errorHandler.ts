import { NextFunction, Request, Response } from 'express';

import {
	BadRequest,
	InternalServerError,
	NotFound,
	NotImplemented,
	ServiceUnavailable,
} from '@overture-stack/maestro-common';

import { logger } from '../utils/logger.js';

/**
 * Converts an unknown error cause into a string representation.
 * @param cause The error cause
 * @returns
 */
const convertErrorDetailsToString = (details: unknown): string => {
	if (typeof details === 'string') {
		return details;
	} else if (details instanceof Error) {
		return details.message;
	} else if (details) {
		return String(details);
	} else {
		return 'Unknown cause';
	}
};

/**
 * A Middleware used to map Error types
 * @param err An Error instance
 * @param req Incoming HTTP Request object
 * @param res HTTP Response Object
 * @param _next The next middleware function
 * @returns An HTTP Response Object with the corresponding HTTP code and message
 */

export const errorHandler = (err: Error, req: Request, res: Response, _next: NextFunction) => {
	logger.error('error handler received error: ', err);
	let status: number;
	let details: string | undefined;
	switch (true) {
		case err instanceof BadRequest:
			status = 400;
			details = err.details;
			break;
		case err instanceof NotFound:
			status = 404;
			details = err.details;
			break;
		case err instanceof InternalServerError:
			status = 500;
			details = err.details;
			break;
		case err instanceof NotImplemented:
			status = 501;
			details = err.details;
			break;
		case err instanceof ServiceUnavailable:
			status = 503;
			details = err.details;
			break;
		default:
			status = 500;
			details = convertErrorDetailsToString(err.cause);
	}

	res.status(status).send({ successful: false, message: err.message, details });
};
