# Kafka Topics

Maestro can listen to Kafka topics to index data as events happen, instead of only on demand through the HTTP API. This page explains how to enable Kafka and how the different topics are used.

Kafka is optional. If you do not configure it, Maestro still works through the HTTP API.

## Enabling Kafka

Kafka is configured through environment variables in your `.env` file. To connect Maestro to a broker, set both of the following:

```bash
# Comma-separated list of brokers in host:port form
MAESTRO_KAFKA_BROKERS=kafka1:9092,kafka2:9092

# Consumer group the Maestro consumer joins
MAESTRO_KAFKA_GROUP_ID=maestro
```

:::info
`MAESTRO_KAFKA_BROKERS` and `MAESTRO_KAFKA_GROUP_ID` are required together. If you set one, you must set the other, or Maestro will fail to start.
:::

Maestro listens on two kinds of topics: one **request topic**, and one **document topic per repository**. Each is described below.

## Request Topic

The request topic carries on-demand indexing instructions. When Maestro receives a request message, it fetches the referenced data from the repository and indexes it, exactly as the HTTP indexing endpoints do.

Configure a single request topic and its dead-letter queue:

```bash
MAESTRO_KAFKA_INDEX_REQUEST_TOPIC=maestro-index-request
MAESTRO_KAFKA_INDEX_REQUEST_DLQ=maestro-index-request-dlq
```

The message payload identifies what to index. Maestro decides the operation from which fields are present:

- **Index a repository** (all organizations within it):

  ```json
  { "repositoryCode": "collab" }
  ```

- **Index an organization or study** (all records within it):

  ```json
  { "repositoryCode": "collab", "studyId": "PACA-CA" }
  ```

- **Index a single record**:

  ```json
  { "repositoryCode": "collab", "studyId": "PACA-CA", "analysisId": "EGAZ000" }
  ```

  To remove a single record from the index instead of indexing it, set `remove` to `true`:

  ```json
  { "repositoryCode": "collab", "studyId": "PACA-CA", "analysisId": "EGAZ000", "remove": true }
  ```

:::info Publishing through the Kafka REST proxy
When you publish a message through the Kafka REST proxy, the payload goes in a record's `value` field, for example `{ "records": [ { "value": { "repositoryCode": "collab", "studyId": "PACA-CA" } } ] }`. The examples above show the payload itself.
:::

If a request message cannot be processed, it is sent to the configured dead-letter queue.

## Document Topics

A document topic carries a full document to index directly. Unlike the request topic, Maestro does not fetch anything; it indexes the document contained in the message. Each repository has its own document topic, and the topic a message arrives on determines which repository it is indexed into.

Configure a document topic and dead-letter queue per repository, using the repository's index (`0`, `1`, and so on):

```bash
MAESTRO_REPOSITORIES_0_KAFKA_DOCUMENT_UPDATE_TOPIC=clinical-data
MAESTRO_REPOSITORIES_0_KAFKA_DOCUMENT_UPDATE_DLQ=clinical-data-dlq
```

### Song document

A message on a Song repository's document topic carries an analysis. Its schema is defined by Song and currently looks like this:

```json
{
  "analysisId": "12314124",
  "studyId": "PEK-AB",
  "state": "PUBLISHED",
  "analysis": {
    "analysisId": "a54378b7-9f3a-4dcc-8378-b79f3a3dcc2a",
    "studyId": "ABC123",
    "analysisState": "PUBLISHED",
    "files": [],
    "analysisType": []
  }
}
```

A document is indexed if its `state` matches one of the values in that repository's `MAESTRO_REPOSITORIES_<n>_SONG_INDEXABLE_STUDY_STATES` (for example `PUBLISHED`). If the `state` does not match and an `analysisId` is present, the document is removed from the index instead. Documents are stored as `file` or `analysis` centric depending on the repository's `SONG_INDEXING_MODE` (see [Index Mappings](/develop/Maestro/reference/index-mappings)).

### Lyric document

A message on a Lyric repository's document topic carries a record and typically looks like this:

```json
{
  "categoryAlias": "donor",
  "categoryId": 3,
  "systemId": "12314124",
  "organization": "ABC-123",
  "entityName": "sample",
  "data": { "name": "ABCD" },
  "isValid": true
}
```

`categoryId` is always present; `categoryAlias` is present only when the Lyric category has an alias. If the repository sets `MAESTRO_REPOSITORIES_<n>_LYRIC_VALID_DATA_ONLY=true`, a document is indexed only when its `isValid` field is `true`, and removed otherwise. When the value is `false`, all documents are indexed regardless of `isValid`.

#### Routing a Lyric document to a repository

`MAESTRO_REPOSITORIES_<n>_LYRIC_CATEGORY_ID` accepts either the category's numeric id or its alias. It is matched against the incoming message by equality: `categoryAlias` is tried first, then `categoryId`.

- One topic can be shared by more than one Lyric repository. Lyric may publish several categories onto one topic, and each repository indexes only the messages whose category it is configured for.
- If more than one repository is configured with the same category value, the message is indexed into all of them. This is a deliberate fan-out, not a misconfiguration.
- If no repository matches, the message is logged and skipped. It is not sent to the dead-letter queue, because a well-formed message that simply is not for any configured repository has nothing to reprocess.
- Song repositories have no category concept; the topic alone routes messages to them.

## Additional Resources

- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [KafkaJS Documentation](https://kafka.js.org/)
