import express, { Router } from 'express';

import healthController from '../controllers/healthController.js';

export const healthCheckRouter: Router = (() => {
	const router = express.Router();

	router.get('/', healthController.health);

	return router;
})();
