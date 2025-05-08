# Maestro Provider

![TypeScript](https://img.shields.io/badge/TypeScript-007ACC?style=for-the-badge&logo=typescript&logoColor=white)
[<img hspace="5" src="https://img.shields.io/badge/License-AGPL--3.0-blue?style=for-the-badge">](https://github.com/overture-stack/lectern/blob/develop/LICENSE)

This package provides indexing functionality for keeping data synchronized from data source repositories like Song and Lyric with search engines like ElasticSearch.

The entry point to this functionality is through the `initializeMaestroProvider`. This function creates a provider that offers methods to configure, manage, and execute tasks related to indexing data from repositories and payloads into a search engine like Elasticsearch. It simplifies the interaction with complex indexing workflows by providing a unified entry point to both automatic data synchronization and manual document indexing.

This package is designed to be imported as an npm dependency.

## Install

To install the `maestro-provider` package, run the following command:

```bash
npm install @overture-provider/maestro-provider
```

## Configuration

To create a Maestro provider use the **initializeMaestroProvider** function. This can be done by providing an object of type **MaestroProviderConfig**.

Here is an example of the configuration:

```bash
import { initializeMaestroProvider, MaestroProviderConfig } from '@overture-stack/maestro-provider';

const config: MaestroProviderConfig = {
  // Configuration properties
};

const maestroProvider = initializeMaestroProvider(config);
```

## Features

1. **Repository Indexing Operations**

   The `initializeMaestroProvider` function provides a set of API functions designed to manage indexing operations for systems that work as data source repositories, such as _Song_ or _Lyric_. These repositories serve as data sources from which _Maestro_ fetches data when the operations are invoked. The resulting provider acts as the bridge to ensure that this data is correctly indexed into a search engine like Elasticsearch.

   It provides bellow operations:

   1. **IndexRepository:** This function is responsible for fetching data from a specified repository and indexing it into a search engine.

      **Use Case:**
      Index all records from a _SongRepository_ into Elasticsearch for searchability.

   2. **IndexOrganization:** This function is similar to `indexRepository`, but it process only the data of an specific organization.

      **Use Case:**
      Index the records from a _SongRepository_ that belongs to _STUDY_ABC_ organization, to allow easy searching and querying within the search engine.

   3. **IndexRecord:** This function is designed to index a single record or document into the search engine. Unlike `indexRepository`, which indexes a collection of data, `indexRecord` is used to index an specific record by its unique ID. This is useful when individual items or updates need to be indexed independently, such as when new data is added, or existing data is updated.

      **Use case:** Index a new _Song_ record or update an existing one in the search engine.

   4. **RemoveIndexRecord:** Thisfunction is used to remove a previously indexed record from the search engine. This function ensures that any obsolete or deleted records are removed from the index to maintain accurate and up-to-date search results. It’s often used when a record is deleted from the repository or when it should no longer be searchable.

      **Use Case:** Remove a deleted _Song_ record from the search index to ensure that users cannot search for it anymore.

2. **Indexing Service Implementation for Payloads**

   Maestro provider also offers a set of indexing operations that do not require fetching data from repositories. Instead, the data is provided directly in the payloads.

   It provides bellow operations:

   1. **CreateIndex:**
      This function is used to create a new index in the search engine. This function is typically called when setting up a new index for the first time or creating a new one to handle a different type of data.

      **Use Case:** Create a new index for storing _Song_ data

   2. **AddData:**
      This function is used to index new data into the search engine. It takes the provided data (e.g., a document, record, or object) and adds it to the index.

      **Use case:** Add a new _Song_ document to the _songs_ index, allowing it to be searched by various fields like title, artist, etc.

   3. **Ping**
      The ping function checks the health and availability of the search engine

      **Use case:** Verify that the Elasticsearch cluster is up and running before performing any indexing operations.

   4. **UpdateData:** This function is used to update an existing document or record in the search engine. It requires identifying the document by a unique ID (e.g., analysis ID) and then updating the relevant fields.

      **Use case:** Update the details of a _Song_ document (e.g., change the artist name or genre) in the Elasticsearch index.

   5. **DeleteData:** This function is used to remove a document or record from the search engine, preventing users from searching for it.

      **Use case:** Delete a _Song_ document from the index when it is no longer available or has been removed from the data source.

   6. **BulkUpsert:** This function performs a bulk operation to either insert or update multiple documents at once. This is highly efficient for scenarios where you need to handle large volumes of data or updates in a single request.

      **Use case:** Perform a bulk operation to insert or update multiple _Song_ records in the Elasticsearch index in one go, improving performance compared to inserting or updating them individually.

## Usage Example

After installing and configuring the Maestro Provider, you can start using it to perform indexing operations.

```bash
import { initializeMaestroProvider, MaestroProviderConfig } from 'maestro-provider';

const config: MaestroProviderConfig = {
// Configure your provider
};

const maestroProvider = initializeMaestroProvider(config);

// Example API usage:
maestroProvider.payload.createIndex(payload);
maestroProvider.payload.bulkUpsert(payload);
```
