# Overview

Maestro's primary function is to organize data from multiple data repositories into a single Elasticsearch index. By collecting data into a single index, Maestro allows upstream services, such as Arranger, to consume the data and expose it to end users for search and exploration.

Maestro listens for changes in its configured repositories and keeps the index in step with them. It can source data from [Song](https://docs.overture.bio/develop/Song/overview) (genomic metadata) and [Lyric](https://docs.overture.bio/develop/Lyric/overview) (tabular data submission), fetching records over HTTP or reacting to events on Kafka.

## Key Features

- **Multiple Repositories:** Maestro can connect to several repositories at once and index them all into one Elasticsearch index. If more than one repository identifies the same file, Maestro aggregates the data from every source into a single index document rather than duplicating it.

    ```mermaid
    graph LR
        SD1[(DB1)]--->SS1[Song 1]
        SD2[(DB2)]--->SS2[Song 2]
        LD[(DB3)]--->LY[Lyric]
        SS1 & SS2 & LY--->M[Maestro]
        M-->ES[Elasticsearch]

    ```

- **Song and Lyric Sources:** Each repository is configured as either a Song or a Lyric source. Song supplies analysis and study metadata; Lyric supplies tabular submission data, routed by category. Maestro indexes both into the same Elasticsearch index.

- **Multiple Indexing Levels:** Maestro can index at three levels: an entire repository, a single organization or study within it, or a single record. For example, to index all records for one organization you can use the following command:

    ```bash
    curl -X POST \
        http://localhost:11235/index/repository/<repositoryCode>/organization/<organization> \
        -H 'Content-Type: application/json' \
        -H 'cache-control: no-cache' \
        -d '{}'
    ```

- **Dynamic Schema Support:** Maestro requires only the base fields of a record to index it, but it also carries along any additional fields defined by a Song analysis schema. When those schemas change, an administrator is responsible for updating and migrating the index mapping.

- **File or Analysis Centric Indexing:** For Song repositories, Maestro can build either file centric or analysis centric documents, selected per repository through configuration.

- **HTTP or Kafka Indexing APIs:** Maestro can receive indexing requests through an HTTP web API or from <a href="https://kafka.apache.org/" target="_blank" rel="noopener noreferrer">Apache Kafka</a> topics for event-driven indexing.

## System Architecture

Maestro organizes data from multiple repositories into a single Elasticsearch index, enabling upstream services like [Arranger](https://docs.overture.bio/develop/Arranger/overview) to consume and expose the data for user search and exploration.

![Maestro Arch](./assets/maestroDev.svg 'Maestro Architecture Diagram')

As part of the larger Overture.bio software suite, Maestro integrates with several services:

* **Song:** Maestro reads analysis and study metadata from one or more Song servers and indexes it into a single index.
* **Lyric:** Maestro reads tabular submission data from Lyric and indexes it alongside Song data in the same index.
* **Elasticsearch:** Maestro builds and maintains Elasticsearch indices; both Elasticsearch 7 and 8 are supported.
* **Apache Kafka:** Optional integration for event-based indexing. Maestro can listen on Kafka topics and trigger indexing operations from the messages it receives.

## Repository Structure

Maestro V5 is a TypeScript project managed as a [pnpm](https://pnpm.io/) monorepo. Each application and package lives in its own folder:

```
.
├── /apps
│   └── /server
└── /packages
    ├── /common
    ├── /indexer-client
    └── /maestro-provider
```

#### apps/server

The main runnable [Express](https://expressjs.com/) server. It exposes the HTTP API routes that connect the Maestro provider to external systems, reads configuration from environment variables, and, when Kafka is configured, runs the consumer that listens for indexing events.

#### packages/common

Shared utilities, reusable functions, and TypeScript type definitions used across the other packages.

#### packages/indexer-client

Abstracts communication with Elasticsearch, supporting both version 7 and version 8 clients.

#### packages/maestro-provider

The core, framework-independent indexing logic: the main indexing operations, repository handling, and rules that fulfil Maestro's business features. It also contains the Kafka consumer used for event-driven indexing.
