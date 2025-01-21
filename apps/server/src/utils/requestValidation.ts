import { RequestHandler } from 'express-serve-static-core';
import { ZodSchema } from 'zod';

import { BadRequest } from '@overture-stack/maestro-common';

import { logger } from '../utils/logger.js';

const LOG_MODULE = 'REQUEST_VALIDATION';

export declare type RequestValidation<TBody, TQuery, TParams> = {
	body?: ZodSchema<TBody>;
	query?: ZodSchema<TQuery>;
	pathParams?: ZodSchema<TParams>;
};

/**
 * Validate the Request using Zod parse
 * @param schema Zod objects used to validate request
 * @returns Throws a Bad Request when validation fails
 */
export function validateRequest<TBody, TQuery, TParams>(
	schema: RequestValidation<TBody, TQuery, TParams>,
	handler: RequestHandler<TParams, unknown, TBody, TQuery>,
): RequestHandler<TParams, unknown, TBody, TQuery> {
	return async (req, res, next) => {
		// Collect all validation errors in an array
		const validationErrors: string[] = [];

		// Validate the body if schema defines it
		if (schema.body) {
			const bodyResult = schema.body.safeParse(req.body);
			if (!bodyResult.success) {
				// Collect errors from body validation
				validationErrors.push(
					...bodyResult.error.errors.map((err) => `Body param '${err.path.join('.')}' is ${err.message}`),
				);
			}
		}

		// Validate the query if schema defines it
		if (schema.query) {
			const queryResult = schema.query.safeParse(req.query);
			if (!queryResult.success) {
				// Collect errors from query validation
				validationErrors.push(
					...queryResult.error.errors.map((err) => `Query param '${err.path.join('.')}' is ${err.message}`),
				);
			}
		}

		// Validate the path parameters if schema defines them
		if (schema.pathParams) {
			const pathResult = schema.pathParams.safeParse(req.params);
			if (!pathResult.success) {
				// Collect errors from path parameters validation
				validationErrors.push(
					...pathResult.error.errors.map((err) => `Path param '${err.path.join('.')}' is ${err.message}`),
				);
			}
		}

		// If there are validation errors, throw a consolidated error
		if (validationErrors.length > 0) {
			logger.error(LOG_MODULE, req.method, req.url, JSON.stringify(validationErrors));
			return next(new BadRequest(validationErrors.join(' | ')));
		}

		// If no errors, proceed with the handler
		return handler(req, res, next);
	};
}
