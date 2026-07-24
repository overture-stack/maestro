import { existsSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

import swaggerJSDoc from 'swagger-jsdoc';

import { name, version } from './manifest.js';

// this enables swagger when Maestro runs as a standalone container
const findServerRoot = (dir: string): string => {
	if (existsSync(path.join(dir, 'src', 'api-docs'))) {
		return dir;
	}

	const parent = path.dirname(dir);
	if (parent === dir) {
		throw new Error('Could not locate apps/server: no src/api-docs found in any parent directory');
	}

	return findServerRoot(parent);
};

const serverRoot = findServerRoot(path.dirname(fileURLToPath(import.meta.url)));

const swaggerDefinition = {
	openapi: '3.0.0',
	info: {
		title: name,
		version,
	},
};

const options = {
	swaggerDefinition,
	// Paths to files containing OpenAPI definitions
	apis: [path.join(serverRoot, 'src/routes/*.ts'), path.join(serverRoot, 'src/api-docs/*.yml')],
};

export default swaggerJSDoc(options);
