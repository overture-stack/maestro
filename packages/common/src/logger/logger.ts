import type { ConsoleLike, LoggerConfig } from '../types';

let loggerInstance: ConsoleLike | null = null;

/**
 * Initializes the logger instance based on the provided configuration
 *
 * If a `logger` is provided in the `config` argument, it will be used to initialize
 * the `loggerInstance`. Otherwise, the `console` object will be used as the default logger.
 *
 * This function ensures that the logger is properly initialized before being used by other
 * parts of the application.
 *
 * @param config
 */
export const initializeLogger = (config?: LoggerConfig): void => {
	loggerInstance = config?.logger || console;
};

/**
 * A logging utility object that provides methods for logging messages at different levels
 *
 * It requires the logger to be initialized first.
 */
export const logger: ConsoleLike = {
	log: (message: unknown, ...args: unknown[]) => {
		if (!loggerInstance) {
			throw new Error('Logger not initialized. Please call initializeLogger first.');
		}
		loggerInstance.log(message, ...args);
	},
	info: (message: unknown, ...args: unknown[]) => {
		if (!loggerInstance) {
			throw new Error('Logger not initialized. Please call initializeLogger first.');
		}
		loggerInstance.info(message, ...args);
	},
	warn: (message: unknown, ...args: unknown[]) => {
		if (!loggerInstance) {
			throw new Error('Logger not initialized. Please call initializeLogger first.');
		}
		loggerInstance.warn(message, ...args);
	},
	error: (message: unknown, ...args: unknown[]) => {
		if (!loggerInstance) {
			throw new Error('Logger not initialized. Please call initializeLogger first.');
		}
		loggerInstance.error(message, ...args);
	},
	debug: (message: unknown, ...args: unknown[]) => {
		if (!loggerInstance) {
			throw new Error('Logger not initialized. Please call initializeLogger first.');
		}
		loggerInstance.debug(message, ...args);
	},
};
