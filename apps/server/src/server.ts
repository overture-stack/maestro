import express from 'express';
import helmet from 'helmet';
import { serve, setup } from 'swagger-ui-express';

import { MaestroProvider } from '@overture-stack/maestro-provider';

import { defaultAppConfig } from './config/provider.js';
import swaggerDoc from './config/swaggerDoc.js';
import { errorHandler } from './middleware/errorHandler.js';
import { healthCheckRouter } from './routes/healthCheck.js';
import { indexerRouter } from './routes/indexer.js';
const maestroProvider = MaestroProvider(defaultAppConfig);
console.log(`ping: ${await maestroProvider.indexerProvider.ping()}`);

// Create Express server
const app = express();

app.use(helmet());

// Swagger route
app.use('/api-docs', serve, setup(swaggerDoc));

app.use('/health', healthCheckRouter);

app.use('/index', indexerRouter);

app.use(errorHandler);
export { app };
