![TypeScript](https://img.shields.io/badge/TypeScript-007ACC?style=for-the-badge&logo=typescript&logoColor=white)
[<img hspace="5" src="https://img.shields.io/badge/chat--with--developers-overture--slack-blue?style=for-the-badge">](http://slack.overture.bio)

## Documentation:

Documentation is hosted on :

- github pages: https://overture-stack.github.io/maestro/
- read the docs: https://maestro-overture.readthedocs.io/en/latest/ (using mkdocs https://docs.readthedocs.io/en/stable/intro/getting-started-with-mkdocs.html)

## Introduction

Maestro was created to enable genomic researchers to enhance their Overture SONG/Lyric by building indexes, elastic search by default, that makes searching Analyses and Studies much more powerful and easier.

### Features:

- Supports indexing from multiple metadata repositories (SONG/Lyric).
- Multiple indexing requests: analysis, study, full repository.
- Event driven indexing.
  - Integration with SONG/Lyric to index published analysis and delete suppressed / unpublished analyses
- Ability to Exclude analysis based on different Ids: Study, Analysis, Donor, Sample Or file.
- Slack web hook integration

## Technologies & libraries:

- Node.js v16.14 or greater
- PNPM package manager
- Elasticsearch 7+
- Apache Kafka
- Testing libraries:
  - Mocha
  - Chai
  - Testcontainers

## Structure

The project is organized as a monorepo workspace, where each application and package is located in its own dedicated folder:

```
apps/
├─ server/
packages/
├─ common/
├─ indexer-client/
├─ maestro-provider/
├─ repository/

```

- **Maestro Server:** The main runnable Express Server. Exposes a set of HTTP API routes that serve as the interface between the Maestro provider and external systems.

- **Maestro Common:** Designed to centralize common utilities, reusable functions, and TypeScript type definitions.

- **Maestro Indexer Client:** Abstracts communication with Elasticsearch clients, supporting both version 7 and version 8.

- **Maestro Provider:** The core features and provider independent logic that is portable and contains the main indexing, rules, notifications logic as specified by the business features.

- **Maestro Repository:** Designed to manage interactions with data source repositories. It serves as the central interface for retrieving data from various repositories, such as **Song** and **Lyric**, ensuring a streamlined and consistent approach to data access.

# Dependencies:

To Successfully run Maestro (as is) you need the following services to be deployed and configure it to use them:

- [Elasticsearch](https://www.elastic.co/products/elasticsearch)
- [Apache Kafka](https://kafka.apache.org/)
- [SONG](https://github.com/overture-stack/SONG)
- [Lyric](https://github.com/overture-stack/lyric)

You can check the sample docker compose files under `./apps/server/docker-compose-es7.dev.yml` for containerized versions of elasticsearch 7 & kafka.
For SONG please check the SONG github repo [here](https://github.com/overture-stack/SONG/tree/develop/dev) on how to run it with docker. Or you can run it as jar.

## How to:

- Swagger API access:
  - http://localhost:11235/api-docs

> Note: if you don't/can't use the Makefile, look inside it for the shell commands and replicate them.

- Compile: `make`
- Test: `make test`
- Run:
  - Source:
    - Development:
      1. `make docker-start-dev` starts the infrastructure containers
         - kafka
         - elastic search
         - other helper tools if you want like kafka rest proxy
      2. `make start` to start application
