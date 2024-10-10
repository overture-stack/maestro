# Overview

Maestro's primary function is to organize data from multiple Song repositories into a single Elasticsearch index. By collecting data into a single index, Maestro allows upstream services, such as Arranger, to consume the data and expose it to end users for search and exploration.

## Key Features

- **Multi Repo Management:** Maestro offers built-in conflict detection and resolution. For instance, if multiple Song repositories identify the same file, Maestro detects this and aggregates the data from all sources into the Elasticsearch index.

    ```mermaid
    graph LR
        SD1[(DB1)]--->SS1[Song 1]
        SD2[(DB2)]--->SS2[Song 2]
        SD3[(DB3)]--->SS3[Song 3]
        SS1 & SS2 & SS3--->M[Maestro]
        M-->ES[Elasticsearch]

    ```

- **Multiple Indexing Levels:** Song repositories have a standard hierarchy: **Repository > Study > Analysis**. Maestro can index at each level. For example, to index all analyses within a specific study, you can use the following command:

    ```bash
    curl -X POST \
        http://localhost:11235/index/repository/<repositoryCode>/study/<studyId> \
        -H 'Content-Type: application/json' \
        -H 'cache-control: no-cache' \
    ```

- **Song Schema Support:** Song utilizes a core data model along with a flexible, user-defined dynamic schema. Maestro requires the base schema fields to index data but also supports indexing of additional fields found within the dynamic schema.
 
- **Index Mapping Migrations:** When changes are introduced to the dynamic schema, administrator(s) must update and migrate the new index mapping.

- **Exclusion Rules:** Maestro supports configurable exclusion rules to omit specific analyses from being indexed based on metadata tags assigned by Song. Study, Analysis, File, Sample, Specimen and Donor IDs can be used to exclude specific analyses.

- **HTTP or Kafka Indexing APIs:** Maestro can process indexing requests via <a href="https://kafka.apache.org/" target="_blank" rel="noopener noreferrer">Apache Kafka</a> or through a standard JSON Web API (HTTP).

## System Architecture

Maestro organizes data from multiple Song repositories into a single Elasticsearch index, enabling upstream services like <a href="/documentation/arranger" target="_blank" rel="noopener noreferrer">Arranger</a> to consume and expose the data for user search and exploration.

![Maestro Arch](./assets/maestroDev.svg 'Maestro Architecture Diagram')

As part of the larger Overture.bio software suite, Maestro integrates with several services:

* **Song:** Maestro natively integrates with Song to index Song metadata into a single index.
* **Elasticsearch:** Maestro is designed to integrate with and build Elasticsearch indices by default.
* **Apache Kafka:** Optional integration for event-based indexing using Kafka messaging queues. Maestro can listen for and trigger indexing operations from specific Kafka topics.
* **Slack:** Integrated for index monitoring notifications.

## Repository Structure

```
.
├── /ci-cd
├── /maestro-app
├── /maestro-domain
```

#### maestro-domain

Contains core features and framework-independent logic that is portable and includes the main indexing, rules, and notifications logic. It has packages such as:
- entities: contains POJOs and entities
- api: the logic that fulfills the business features
- ports: contains the interfaces needed by the api to communicate with anything outside the indexing context

#### maestro-app

This is the main runnable Spring Boot app. It contains the infrastructure and adapters (ports implementations) needed to connect the domain with external services like Elasticsearch, Song web clients, and configuration files. It also includes Spring framework configurations to keep technologies outside of the domain.
