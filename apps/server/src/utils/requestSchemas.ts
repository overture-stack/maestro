import type { ParamsDictionary } from 'express-serve-static-core';
import type { ParsedQs } from 'qs';
import { z } from 'zod';

import { RequestValidation } from './requestValidation.js';

export interface indexRepositoryPathParams extends ParamsDictionary {
	repositoryCode: string;
}

export interface indexOrganizationPathParams extends ParamsDictionary {
	repositoryCode: string;
	organization: string;
}
export interface indexRecordPathParams extends ParamsDictionary {
	repositoryCode: string;
	organization: string;
	id: string;
}

const repositoryCodeSchema = z.string().trim().min(1);
const organizationSchema = z.string().trim().min(1);
const recordIdSchema = z.string().trim().min(1);

export const indexRepositoryRequestschema: RequestValidation<object, ParsedQs, indexRepositoryPathParams> = {
	pathParams: z.object({ repositoryCode: repositoryCodeSchema }),
};

export const indexOrganizationRequestschema: RequestValidation<object, ParsedQs, indexOrganizationPathParams> = {
	pathParams: z.object({ repositoryCode: repositoryCodeSchema, organization: organizationSchema }),
};

export const indexRecordRequestschema: RequestValidation<object, ParsedQs, indexRecordPathParams> = {
	pathParams: z.object({ repositoryCode: repositoryCodeSchema, organization: organizationSchema, id: recordIdSchema }),
};
