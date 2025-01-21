import { getServerConfig } from './config/envConfig.js';
import { app } from './server.js';
import { logger } from './utils/logger.js';

const { nodeEnv, port, openApiPath } = getServerConfig();

const server = app.listen(port, () => {
	logger.log(`Server started. Running in "${nodeEnv}" mode. Listening to port ${port}`);

	if (nodeEnv === 'development') {
		logger.log(`Swagger API Docs are available at http://localhost:${port}/${openApiPath}`);
	}
});

const onCloseSignal = () => {
	logger.log('sigint received, shutting down');
	server.close(() => {
		logger.log('server closed');
		process.exit();
	});
	setTimeout(() => process.exit(1), 10000).unref(); // Force shutdown after 10s
};

process.on('SIGINT', onCloseSignal);
process.on('SIGTERM', onCloseSignal);
