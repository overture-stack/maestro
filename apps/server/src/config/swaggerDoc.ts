import swaggerJSDoc from 'swagger-jsdoc';

import { name, version } from './manifest.js';

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
	apis: ['./src/routes/*.ts', './src/api-docs/*.yml'],
};

export default swaggerJSDoc(options);
