# Setup

<!-- Maintainer note (2026-07-28): these instructions target Maestro V5, the TypeScript
`@overture-stack/maestro-server`. At time of writing V5 lives on a development branch and is
not yet the repository default; until it is merged, clone the V5 branch rather than the default
branch. Remove this note once V5 is the default branch on GitHub. -->

## Prerequisites

Before you begin, ensure you have the following installed on your system:

- [Node.js](https://nodejs.org/) v22 or higher
- [pnpm](https://pnpm.io/installation) package manager
- [Docker](https://www.docker.com/products/docker-desktop/) (v4.39.0 or higher), used to run the supporting services

## Developer Setup

This guide will walk you through setting up a complete development environment, including Maestro and the services it depends on.

### Setting up supporting services

Maestro indexes data into Elasticsearch and can react to events on Kafka. The Maestro repository ships a Docker Compose file that starts both for local development.

1. Clone Maestro and move into its directory:

   ```bash
   git clone https://github.com/overture-stack/maestro.git
   cd maestro
   ```

2. Start the infrastructure containers (Elasticsearch and Kafka):

   ```bash
   make docker-start-dev
   ```

   <details>
   <summary>**Click here for a detailed breakdown**</summary>

   This command starts the infrastructure services Maestro needs during development, defined in `apps/server/docker-compose-es7.dev.yml`:

   | Service       | Port   | Description                                     | Purpose in Maestro Development                                    |
   | ------------- | ------ | ----------------------------------------------- | ---------------------------------------------------------------- |
   | Elasticsearch | `9200` | Distributed search and analytics engine         | Provides the index Maestro reads from and writes to              |
   | Kafka broker  | `9092` | Distributed event streaming platform            | Carries the messages that trigger event-driven indexing          |
   | Zookeeper     | `2181` | Coordination service for Kafka                  | Required by the Kafka broker                                     |
   | Kafka REST proxy | `8082` | HTTP interface to Kafka                       | Lets you publish test messages to topics over HTTP               |

   - Ensure these ports are free on your system before starting the environment.
   - You may need to adjust the ports in the Docker Compose file if you have conflicts with existing services.
   - Song and Lyric are not started by this file. To index real data, run [Song](https://docs.overture.bio/develop/Song/overview) or [Lyric](https://docs.overture.bio/develop/Lyric/overview) separately and point Maestro at them through configuration.

   To stop the infrastructure containers again, run `make docker-stop-dev`.

   </details>

### Configuring Maestro

Maestro is configured entirely through environment variables, all prefixed with `MAESTRO_`. A template listing the available variables ships as `apps/server/.env.example`.

1. Create a `.env` file in `apps/server/` based on the template:

   ```bash
   cp apps/server/.env.example apps/server/.env
   ```

2. Update the Elasticsearch, Song, Lyric, and Kafka sections in `apps/server/.env` to match your environment. At minimum, Maestro needs an Elasticsearch node and at least one repository configured.

:::info
For a full description of the configuration variables, see the reference pages for [indexing](./reference/01-indexing-data.md), [index mappings](./reference/03-index-mappings.md), and [Kafka topics](./reference/04-kafka-topics.md).
:::

### Running the Development Server

1. Install dependencies and build all packages:

   ```bash
   pnpm install
   pnpm run build:all
   ```

    <details>
    <summary>**Click here for an explanation of the commands above**</summary>

   - `pnpm install`: installs the dependencies for every package in the monorepo.
   - `pnpm run build:all`: compiles all of the TypeScript packages and the server application.

   The repository also provides a `Makefile` that wraps these commands. Running `make compile` is equivalent to `pnpm install && pnpm run build:all`, and `make start` runs the server. Open the `Makefile` to see the full set of targets, which also includes REST and Kafka helper commands.

    </details>

2. Start the Maestro server:

   ```bash
   pnpm run start:dev
   ```

   :::tip
   Ensure you are running Node.js v22 or higher. To check, run `node --version`. You should see something similar to the following:

   ```bash
   v22.11.0
   ```

   :::

### Verification

After installing and configuring Maestro, verify that the system is functioning correctly:

1. **Check Server Health**

   ```bash
   curl -s -o /dev/null -w "%{http_code}" "http://localhost:11235/health"
   ```

   - Expected result: Status code `200`. The endpoint returns a JSON body reporting uptime, a status message, and a timestamp.
   - Troubleshooting:
     - Ensure the Maestro server is running
     - Check you're using the correct port (default is 11235)
     - Verify no firewall issues are blocking the connection

2. **Check the Swagger UI**
   - Navigate to `http://localhost:11235/api-docs` in a web browser
   - Expected result: Swagger UI page with a list of available API endpoints
   - Troubleshooting:
     - Check browser console for error messages
     - Verify you're using the correct URL

:::info Need Help?
If you encounter any issues or have questions about our API, please don't hesitate to reach out through our [**support page**](https://docs.overture.bio/community/support) or our [**discussion forum**](https://github.com/overture-stack/docs/discussions?discussions_q=).
:::

:::warning
This guide is meant to demonstrate the configuration and usage of Maestro for development purposes and is not intended for production. If you use this in any public or production environment, review the Elasticsearch authentication and Kafka settings and do not rely on the development defaults.
:::
