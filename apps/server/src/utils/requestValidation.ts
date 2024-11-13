import { RequestHandler } from 'express-serve-static-core';
import { ZodError, ZodSchema } from 'zod';

import { BadRequest, InternalServerError } from '@overture-stack/maestro-common';

import { logger } from '../utils/logger.js';

export declare type RequestValidation<TBody, TQuery, TParams> = {
	body?: ZodSchema<TBody>;
	query?: ZodSchema<TQuery>;
	pathParams?: ZodSchema<TParams>;
};

/**
 * Validate the body using Zod parse
 * @param schema Zod objects used to validate request
 * @returns Throws a Bad Request when validation fails
 */
export function validateRequest<TBody, TQuery, TParams>(
	schema: RequestValidation<TBody, TQuery, TParams>,
	handler: RequestHandler<TParams, unknown, TBody, TQuery>,
): RequestHandler<TParams, unknown, TBody, TQuery> {
	const LOG_MODULE = 'REQUEST_VALIDATION';
	return async (req, res, next) => {
		try {
			if (schema.body) {
				schema.body.parse(req.body);
			}

			if (schema.query) {
				schema.query.parse(req.query);
			}

			if (schema.pathParams) {
				schema.pathParams.parse(req.params);
			}

			return handler(req, res, next);
		} catch (error) {
			if (error instanceof ZodError) {
				const errorMessages = error.errors.map((issue) => `${issue.path.join('.')} is ${issue.message}`).join(' | ');
				logger.error(LOG_MODULE, req.method, req.url, JSON.stringify(errorMessages));
				next(new BadRequest(errorMessages));
			} else {
				logger.error(LOG_MODULE, req.method, req.url, 'Internal Server Error');
				next(new InternalServerError());
			}
		}
	};
}
