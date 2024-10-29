import { NextFunction, Request, Response } from 'express';

import {
	BadRequest,
	InternalServerError,
	NotFound,
	NotImplemented,
	ServiceUnavailable,
} from '@overture-stack/maestro-common';

/**
 * A Middleware used to map Error types
 * @param err An Error instance
 * @param req Incoming HTTP Request object
 * @param res HTTP Response Object
 * @param _next The next middleware function
 * @returns An HTTP Response Object with the corresponding HTTP code and message
 */

export const errorHandler = (err: Error, req: Request, res: Response, _next: NextFunction) => {
	console.error('error handler received error: ', err);
	let status: number;
	const customizableMsg = err.message;
	const details = err.cause;
	switch (true) {
		case err instanceof BadRequest:
			status = 400;
			break;
		case err instanceof NotFound:
			status = 404;
			break;
		case err instanceof InternalServerError:
			status = 500;
			break;
		case err instanceof NotImplemented:
			status = 501;
			break;
		case err instanceof ServiceUnavailable:
			status = 503;
			break;
		default:
			status = 500;
	}

	res.status(status).send({ error: err.name, message: customizableMsg, details: details });
};
