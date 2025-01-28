import type { ConsoleLike } from '../types';

let loggerInstance: ConsoleLike = console;

/**
 * Configure the logger instance based on the provided configuration
 *
 * @param config
 */
export const setLogger = (logger: ConsoleLike): void => {
	loggerInstance = logger;
};

/**
 * A logging utility object that provides methods for logging messages at different levels
 * Uses custom logger if provided; otherwise, the default `console` logger will be used
 */
export const logger: ConsoleLike = {
	log: (message: unknown, ...args: unknown[]) => {
		return loggerInstance.log(message, ...args);
	},
	info: (message: unknown, ...args: unknown[]) => {
		return loggerInstance.info(message, ...args);
	},
	warn: (message: unknown, ...args: unknown[]) => {
		return loggerInstance.warn(message, ...args);
	},
	error: (message: unknown, ...args: unknown[]) => {
		return loggerInstance.error(message, ...args);
	},
	debug: (message: unknown, ...args: unknown[]) => {
		return loggerInstance.debug(message, ...args);
	},
};
