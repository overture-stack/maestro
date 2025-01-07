import { BadRequest } from '@overture-stack/maestro-common';
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

		if (!repoCode) {
			throw new BadRequest();
		}

		const result = await maestroProvider.api.indexRepository(repoCode);
		// TODO: format result, return corresponding status code
		res.status(200).send(result);
	} catch (error) {
		next(error);
	}
});

const indexOrganization = validateRequest(indexOrganizationRequestschema, async (req, res, next) => {
	try {
		const repoCode = req.params.repositoryCode;
		const organization = req.params.organization;

		if (!repoCode) {
			throw new BadRequest();
		}

		const result = await maestroProvider.api.indexOrganization(repoCode, organization);
		// TODO: format result, return corresponding status code
		res.status(200).send(result);
	} catch (error) {
		next(error);
	}
});

const indexRecord = validateRequest(indexRecordRequestschema, async (req, res, next) => {
	try {
		const repoCode = req.params.repositoryCode;
		const organization = req.params.organization;
		const id = req.params.id;

		if (!repoCode) {
			throw new BadRequest();
		}

		const result = await maestroProvider.api.indexRecord(repoCode, organization, id);
		// TODO: format result, return corresponding status code
		res.status(200).send(result);
	} catch (error) {
		next(error);
	}
});

export default {
	indexRepository,
	indexOrganization,
	indexRecord,
};
