import { initializeMaestroProvider } from '@overture-stack/maestro-provider';

import { defaultAppConfig } from '../config/provider.js';
import {
	indexOrganizationRequestschema,
	indexRecordRequestschema,
	indexRepositoryRequestschema,
} from '../utils/requestSchemas.js';
import { validateRequest } from '../utils/requestValidation.js';

const maestroProvider = initializeMaestroProvider(defaultAppConfig);

const indexRepository = validateRequest(indexRepositoryRequestschema, async (req, res, next) => {
	try {
		const repoCode = req.params.repositoryCode;

		const result = await maestroProvider.api?.indexRepository(repoCode);

		if (result?.successful) {
			// Accepted
			res.status(202).send(result);
		} else {
			// Bad Request
			res.status(400).send(result);
		}
	} catch (error) {
		next(error);
	}
});

const indexOrganization = validateRequest(indexOrganizationRequestschema, async (req, res, next) => {
	try {
		const repoCode = req.params.repositoryCode;
		const organization = req.params.organization;

		const result = await maestroProvider.api?.indexOrganization(repoCode, organization);
		if (result?.successful) {
			// Accepted
			res.status(202).send(result);
		} else {
			// Bad Request
			res.status(400).send(result);
		}
	} catch (error) {
		next(error);
	}
});

const indexRecord = validateRequest(indexRecordRequestschema, async (req, res, next) => {
	try {
		const repoCode = req.params.repositoryCode;
		const organization = req.params.organization;
		const id = req.params.id;

		const result = await maestroProvider.api?.indexRecord(repoCode, organization, id);
		if (result?.successful) {
			// Accepted
			res.status(202).send(result);
		} else {
			// Bad Request
			res.status(400).send(result);
		}
	} catch (error) {
		next(error);
	}
});

export default {
	indexRepository,
	indexOrganization,
	indexRecord,
};
