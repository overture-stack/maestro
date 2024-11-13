import { pino } from 'pino';

import type { ConsoleLike } from '@overture-stack/maestro-common';

const pinoLogger = pino({
	level: 'info',
	transport: {
		target: 'pino-pretty',
	},
});

export const logger: ConsoleLike = {
	log: (message, ...args) => pinoLogger.info({ msg: message, ...args }),
	info: (message, ...args) => pinoLogger.info({ msg: message, ...args }),
	warn: (message, ...args) => pinoLogger.warn({ msg: message, ...args }),
	error: (message, ...args) => pinoLogger.error({ msg: message, ...args }),
	debug: (message, ...args) => pinoLogger.debug({ msg: message, ...args }),
};

export const setLogLevel = (level: string) => {
	pinoLogger.level = level;
};
