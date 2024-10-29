import express, { Router } from 'express';
import { serve, setup } from 'swagger-ui-express';

import swaggerDoc from '../config/swaggerDoc.js';

export const openAPIRouter: Router = (() => {
	const router = express.Router();

	router.use('/', serve, setup(swaggerDoc));

	return router;
})();
