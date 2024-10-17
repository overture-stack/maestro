import express from 'express';
import helmet from 'helmet';
import { serve, setup } from 'swagger-ui-express';

// import pingRouter from './routes/ping.js';
// const serverConfig = getServerConfig();
import {} from '@overture-stack/maestro-common';
import { MaestroProvider } from '@overture-stack/maestro-provider';

// import { errorHandler, provider } from '@overture-stack/lyric';
// import { getServerConfig } from './config/envConfig.js';
import { defaultAppConfig } from './config/provider.js';
import swaggerDoc from './config/swaggerDoc.js';
import { healthCheckRouter } from './routes/healthCheck.js';
const lyricProvider = MaestroProvider(defaultAppConfig);
console.log(`ping: ${await lyricProvider.indexerProvider.ping()}`);

// Create Express server
const app = express();

app.use(helmet());

// Ping Route
// app.use('/ping', pingRouter);

// Lyric Routes
// app.use('/audit', lyricProvider.routers.audit);
// app.use('/category', lyricProvider.routers.category);
// app.use('/data', lyricProvider.routers.submittedData);
// app.use('/dictionary', lyricProvider.routers.dictionary);
// app.use('/submission', lyricProvider.routers.submission);

// Swagger route
app.use('/api-docs', serve, setup(swaggerDoc));

app.use('/health', healthCheckRouter);

// app.use(errorHandler);
export { app };
