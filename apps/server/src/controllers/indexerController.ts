import { Request, Response } from 'express';

import { NotImplemented } from '@overture-stack/maestro-common';

const indexRepository = (req: Request, res: Response) => {
	throw new NotImplemented();
};

const indexOrganization = (req: Request, res: Response) => {
	throw new NotImplemented();
};

const indexRecord = (req: Request, res: Response) => {
	throw new NotImplemented();
};

export default { indexRepository, indexOrganization, indexRecord };
