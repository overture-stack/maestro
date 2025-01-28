# Source Code

Source Code is hosted on [Github](https://github.com/overture-stack/maestro).

This project is intended to be managed as a monorepo using [PNPM](https://pnpm.io/) package manager.

# Dependencies

To run Maestro you need the following services running:

- [Elasticsearch](https://www.elastic.co/products/elasticsearch) version 7 and version 8 (Arranger support for the latter is currently a work in progress).
- [SONG](https://www.overture.bio/products/song) to use as a Genomic metadata source.
- [Lyric](https://github.com/overture-stack/lyric) to use as a metadata source.
- Optional: [Apache Kafka](https://kafka.apache.org/) (if you want event driven integration with SONG).

# Configurations

Configuration in the code repository is managed using the `apps/server/.env.schema` file as a template. To set up your environment:

1. Create a new `.env` file in the `apps/server/` directory based on the `.env.schema` template.
2. Update the relevant sections in the `.env` file to configure connections for Elasticsearch, SONG, Lyric and Kafka according to your specific setup.

# Running Locally

> [!TIP]
> Maestro provides a `Makefile` to simplify the execution of key commands and to easily spin up containerized versions of required dependent services.
> To view the list of available commands, run: `make help`

## Source Code (No Docker)

Ensure you have **Node.js v16.14** or higher and **pnpm** installed, all required dependencies (see [Dependencies](#dependencies)) running, and a properly configured `.env` file (see [Configurations](#configurations)) according to your environment and needs.

Once ready, you can execute the following set of commands to install dependencies, compile the code and start the application:

```bash
pnpm install
pnpm run build:all
cd server/apps
pnpm run start:dev
```
