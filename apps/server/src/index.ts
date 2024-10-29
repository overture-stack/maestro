import { getServerConfig } from './config/envConfig.js';
import { app } from './server.js';

const { nodeEnv, port, openApiPath } = getServerConfig();

const server = app.listen(port, () => {
	console.log(`Server started. Running in "${nodeEnv}" mode. Listening to port ${port}`);

	if (nodeEnv === 'development') {
		console.log(`Swagger API Docs are available at http://localhost:${port}/${openApiPath}`);
	}
});

const onCloseSignal = () => {
	console.log('sigint received, shutting down');
	server.close(() => {
		console.log('server closed');
		process.exit();
	});
	setTimeout(() => process.exit(1), 10000).unref(); // Force shutdown after 10s
};

process.on('SIGINT', onCloseSignal);
process.on('SIGTERM', onCloseSignal);
