# Maestro Server

> [!NOTE]
> This package is likely not the one you want to use in your project, it is primarily used as a dependency within the Maestro monorepo. [@overture-stack/maestro-provider](https://www.npmjs.com/package/@overture-stack/maestro-provider) is what you are most likely to want to use.

![TypeScript](https://img.shields.io/badge/TypeScript-007ACC?style=for-the-badge&logo=typescript&logoColor=white)
[![NPM Version](https://img.shields.io/npm/v/@overture-stack/maestro-server?color=%23cb3837&style=for-the-badge&logo=npm)](https://www.npmjs.com/package/@overture-stack/maestro-server)

## Requirements

- [Node.js](https://nodejs.org/en/)
- [Elasticsearch Service](https://www.elastic.co/elasticsearch)
- [Apache Kafka Service](https://kafka.apache.org/) (optional)
- [Overture Lyric](https://github.com/overture-stack/lyric) (optional)
- [Overture SONG](https://github.com/overture-stack/SONG) (optional)

## Getting started

This application uses `pnpm` as monorepo manager, to set up this project locally, follow these steps from the root folder

### Install Dependencies:

Run the following command to install all necessary dependencies:

```shell
pnpm i
```

### Build the Workspace

Use the following command to build the entire workspace:

```shell
pnpm build:all
```

### Dependant services

Maestro Server requires an instance of ElasticSearch (Supports version 7 or version 8).

Optionally it can be configured to use Apache kafka as a event streaming platform.

To facilitate development we provide a docker-compose file to start elasticsearch and kafka services:

To use Elasticsearch v7 run:

```shell
docker-compose -f docker-compose-es7.dev.yml up -d
```

Or, for Elasticsearch v8:

```shell
docker-compose -f docker-compose-es8.dev.yml up -d
```

### Environment Variables:

Refer to the Environment Variables template found at `.env.example` to configure the required environment variables.

### Run Server in Development Mode:

Once the build is complete, start the server in development mode using the command:

```shell
pnpm run start:dev
```

By default, the server runs on port 11235.

6. Interact with API Endpoints:

A Swagger web interface is available to interact with the API endpoints. Access it at http://localhost:11235/api-docs/.
