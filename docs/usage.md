# Workflow

Maestro does try to create an index based on the _dynamic mapping_ rules that can be customized to suit your purposes, evolving the mapping is the user's responsibility to do through Elasticsearch APIs.

## Mapping Changes

Since Maestro works with a dynamic analysis schema, that can change in runtime the index will need to adapt and Maestro
supports that since it can capture and pass along all the new fields added to the analyses in SONG/Lyric, it tries to stay
out of the way as possible. This dynamic model requires a proper migration process to be practiced by the users to allow
their Index to evolve along their model, the process will be something like:

- Index is created (either manually or by Maestro)
- Maestro runs and start indexing analysis.
- SONG/Lyric introduces new analysis types with new fields
- Maestro will continue working and indexing those documents but new fields won't be indexed yet.
- Index mapping needs update due to new analysis types, or change of some structure etc.. :
  - Create new index with the updated mapping
  - Reindex your data (you can point maestro to your new index and trigger indexing on full repositories to do that,
    or you can use `/reindex` API in Elasticsearch).
  - switch your Aliases if it still points to the old index
  - now that your data is migrated Maestro will be indexing based on the new mapping.

# How to Index

Maestro can be used through either a message driven kafka topic or an HTTP json API

## Http API

- Index by Organization/StudyId:

`POST http://maestro.host:11235/index/repository/<repo>/organization/<organization or studyId>`

```bash
curl -X POST \
	http://localhost:11235/index/repository/collab/organization/BASH-AR \
	-H 'Content-Type: application/json' \
	-H 'cache-control: no-cache' \
	-d '{}'
```

- Index by Analysis:

`POST http://maestro.host:11235/index/repository/<repo>/organization/<organization or studyId>/id/<analysisId>`

```bash
	curl -X POST \
	http://localhost:11235/index/repository/collab/organization/BASH-AR/id/ad7cabf8-df45-40fe6 \
	-H 'Content-Type: application/json' \
	-H 'cache-control: no-cache'
```

- Index an entire repository:

`POST http://maestro.host:11235/index/repository/<repo-code>`

```bash
	curl -X POST \
	http://localhost:11235/index/repository/collab \
	-H 'Content-Type: application/json' \
	-H 'cache-control: no-cache'
```

## Kafka topics

Maestro can be configured to listen to Kafka topics as described in the Running Configurations section.

```yaml
MAESTRO_KAFKA_SERVER=http://kafka:9092
```

The `MAESTRO_KAFKA_SERVER` configuration specifies the Kafka server(s) to connect to.

There are two types of topics that can be configured, depending on your needs:

### Request topics

Request topics are used to send on-demand messages instructing Maestro to perform a specific action on a given `repositoryCode`.

Configuration example:

```yaml
MAESTRO_KAFKA_INDEX_REQUEST_TOPIC=maestro_index_request
MAESTRO_KAFKA_INDEX_REQUEST_DLQ=maestro_index_request_dlq
```

The message body for these topics should follow one of the formats below

- Analysis:

```json
{ "value": { "repositoryCode": "collab", "studyId": "PEK-AB", "analysisId": "EGAZ000", "remove": true } }
```

- Study:

```json
{ "value": { "repositoryCode": "collab", "studyId": "PEK-AB" } }
```

- Full repository:

```json
{ "value": { "repositoryCode": "aws" } }
```

### Song documents specific Topic

You can send an entire document for indexing using a Kafka message.

Use following configuration on a Song repository, example:

```yaml
MAESTRO_REPOSITORIES_0_KAFKA_ANALYSIS_MESSAGE_TOPIC=song_analysis
MAESTRO_REPOSITORIES_0_KAFKA_ANALYSIS_MESSAGE_DLQ=song_analysis_dlq
```

The message schemas are governed by SONG but they currently look like this:

```json
{ "value": { "analysisId": "12314124", "studyId": "PEK-AB", "songServerId": "collab", "state": "PUBLISHED" } }
```

### Lyric Specific Topic

You can send an entire document for indexing using a Kafka message.

Use following configuration on a Song repository, example:

```yaml
MAESTRO_REPOSITORIES_1_KAFKA_ANALYSIS_MESSAGE_TOPIC=clinical_data
MAESTRO_REPOSITORIES_1_KAFKA_ANALYSIS_MESSAGE_DLQ=clinical_data_dlq
```

The message schemas are governed by LYRIC but they currently look like this:

```json
{ "value": { "id": "12314124", "organization": "ABC-123", "serverId": "clinical", "data": { "name": "ABCD" } } }
```
