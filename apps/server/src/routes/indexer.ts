import express, { Router } from 'express';

import indexerController from '../controllers/indexerController.js';

export const indexerRouter: Router = (() => {
	const router = express.Router();

	router.post('/repository/:repositoryCode', indexerController.indexRepository);
	router.post('/repository/:repositoryCode/organization/:organization', indexerController.indexOrganization);
	router.post('/repository/:repositoryCode/organization/:organization/id/:id', indexerController.indexRecord);

	return router;
})();
