export interface ConsoleLike {
	log(message: unknown, ...args: unknown[]): void;
	info(message: unknown, ...args: unknown[]): void;
	warn(message: unknown, ...args: unknown[]): void;
	error(message: unknown, ...args: unknown[]): void;
	debug(message: unknown, ...args: unknown[]): void;
}
