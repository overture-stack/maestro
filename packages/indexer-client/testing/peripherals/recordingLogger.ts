import { type ConsoleLike, setLogger } from '@overture-stack/maestro-common';

/**
 * One call to the logger: its level, its message and any structured fields passed after the message.
 */
export type LogEntry = { args: unknown[]; level: keyof ConsoleLike; message: unknown };

/**
 * Routes Maestro's logger into `entries` until `restore` is called, which hands logging back to `console`, the
 * logger's default.
 */
export const recordLogs = (): { entries: LogEntry[]; restore: () => void } => {
	const entries: LogEntry[] = [];
	const record =
		(level: keyof ConsoleLike) =>
		(message: unknown, ...args: unknown[]): void => {
			entries.push({ args, level, message });
		};
	setLogger({
		debug: record('debug'),
		error: record('error'),
		info: record('info'),
		log: record('log'),
		warn: record('warn'),
	});
	return { entries, restore: () => setLogger(console) };
};
