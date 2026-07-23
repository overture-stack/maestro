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
  - Reindex your data (you can point Maestro to your new index and trigger indexing on full repositories to do that,
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

The `MAESTRO_KAFKA_BROKERS` configuration specifies the Kafka broker(s) to connect to. It supports one or more Kafka brokers specified in the format `host:port`, separated by commas.

Example with multiple brokers:

```yaml
MAESTRO_KAFKA_BROKERS=kafka1:9092,kafka2:9092
```

There are two types of topics that can be configured, depending on your needs:

### Request topics

Request topics are used to send on-demand messages instructing Maestro to fetch the documents from the repository source and perform a specific indexing action.

The following applies for both SONG and Lyric instances.

Configuration example:

```yaml
MAESTRO_KAFKA_INDEX_REQUEST_TOPIC=maestro_index_request
MAESTRO_KAFKA_INDEX_REQUEST_DLQ=maestro_index_request_dlq
```

The message body for these topics should follow one of the formats below

- Analysis:

```json
{ "value": { "repositoryCode": "collab", "studyId": "PEK-AB", "analysisId": "EGAZ000", "remove": false } }
```

To remove an analysis, set the `remove` field to `true`

- Study:

```json
{ "value": { "repositoryCode": "collab", "studyId": "PEK-AB" } }
```

- Full repository:

```json
{ "value": { "repositoryCode": "collab" } }
```

### Documents specific Topic

You can send an entire document for indexing using a Kafka message.

Use following configuration on a Song or Lyric repository, example:

```yaml
MAESTRO_REPOSITORIES_0_KAFKA_DOCUMENT_UPDATE_TOPIC=topic_analysis
MAESTRO_REPOSITORIES_0_KAFKA_DOCUMENT_UPDATE_DLQ=topic_analysis_dlq
```

#### SONG document

To index a document in a SONG repository. While the message schemas are defined by SONG, they currently follow this structure:

```json
{
	"value": {
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
}
```

A document will be indexed if its `state` matches one of the values specified in the configuration property `MAESTRO_REPOSITORIES_0_SONG_INDEXABLE_STUDY_STATES` (e.g., `PUBLISHED`). If the `state` does not match and the `analysisId` is provided, the document will be removed.

Documents can be converted to either `fileCentric` or `analysisCentric` modes, depending on the repository configuration. For more information, see the section titled "SONG Repository Indexing Modes."

#### Lyric document

To index a document in a Lyric repository, the message structure typically looks like this:

```json
{
	"value": {
		"categoryAlias": "donor",
		"categoryId": 3,
		"systemId": "12314124",
		"organization": "ABC-123",
		"entityName": "sample",
		"data": { "name": "ABCD" },
		"isValid": true
	}
}
```

`categoryId` is always present; `categoryAlias` is present only if the Lyric category has an alias assigned.

If the configuration property `MAESTRO_REPOSITORIES_1_LYRIC_VALID_DATA_ONLY` is set to `true` a document will only be indexed if its `isValid` fiels is `true`; otherwise, it will be removed.

If the configuration property is set to `false`, all documents will be indexed regardless of their `isValid` status.

#### Routing a document to a repository

`MAESTRO_REPOSITORIES_N_LYRIC_CATEGORY_ID` accepts either the category's numeric id or its alias, matched against the incoming message by equality, not shape, so a numeric-looking alias is never confused with a plain id.

A Kafka topic can be shared by more than one Lyric repository, or by only one:

- Every Lyric repository always requires a match: `categoryAlias` is tried first, falling back to `categoryId`. A repository matches if its `LYRIC_CATEGORY_ID` equals either field. This applies even when only one Lyric repository is configured for the topic, since Lyric's own publishing isn't under Maestro's control, it may fan out several categories onto one topic while Maestro only indexes a subset.
- More than one repository configured with the same value: the message is indexed into all of them, a deliberate fan-out, not a misconfiguration.
- No match: logged and skipped, not sent to the dead-letter queue. A well-formed message that simply isn't for any configured repository has nothing to reprocess, this is expected whenever a topic carries categories this deployment isn't configured to index.
- SONG repositories have no category concept: topic alone routes to them, same as before.

## SONG Repository Indexing Modes

The structure of SONG documents indexed in the system is controlled by the `MAESTRO_REPOSITORIES_0_SONG_INDEXING_MODE` environment variable, which accepts values `file` or `analysis`. This setting controls how incoming analysis data is transformed before being stored in your index. :

- Analysis-centric indexing (default):

```json
MAESTRO_REPOSITORIES_0_SONG_INDEXING_MODE=analysis
```

- File-centric indexing:

```json
MAESTRO_REPOSITORIES_0_SONG_INDEXING_MODE=file
```
