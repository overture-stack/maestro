
# Workflow

Maestro does try to create an index based on the configuration but it has basic initial mapping, 
that mapping is not configurable in Runtime, evolving the mapping is the user's responsibility to do through Elasticsearch APIs.

## Mapping Changes

Since Maestro works with a dynamic analysis schema, that can change in runtime the index will need to adapt and Maestro
supports that since it can capture and pass along all the new fields added to the analyses in SONG, it tries to stay
out of the way as possible. This dynamic model requires a proper migration process to be practiced by the users to allow
their Index to evolve along their model, the process will be something like: 

- Index is created (either manually or by Maestro)
- Maestro runs and start indexing analysis.
- SONG introduces new analysis types with new fields
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

- Index by Study:

`POST http://maestro.host:11235/index/repository/<repo>/study/<studyId>`

```bash 
curl -X POST \
	http://localhost:11235/index/repository/collab/study/BASH-AR \
	-H 'Content-Type: application/json' \
	-H 'cache-control: no-cache' \
	-d '{}'
```

- Index by Analysis: 

`POST http://maestro.host:11235/index/repository/<repo>/study/<studyId>/analysis/<analysisId>`

```bash
	curl -X POST \
	http://localhost:11235/index/repository/collab/study/BASH-AR/analysis/ad7cabf8-df45-40fe6 \
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

Maestro can be configured as mentioned under the running configurations section to listen to kafka topics 

```yaml
spring:
  application:
    name: maestro
  output.ansi.enabled: ALWAYS
  cloud:
    stream:
      # kafka integration with song (remove this key to disable kafka)
      kafka:
        binder:
          brokers: localhost:9092
        bindings:
          songInput:
            consumer:
              enableDlq: true
              dlqName: maestro_song_analysis_dlq
              autoCommitOnError: true
              autoCommitOffset: true
          input:
            consumer:
              enableDlq: true
              dlqName: maestro_index_requests_dlq
              autoCommitOnError: true
              autoCommitOffset: true
      bindings:
        input:
          # we don't specify content type because @StreamListener will handle that
          destination: maestro_index_requests
          group: requestsConsumerGrp
          consumer:
            maxAttempts: 1
        songInput:
          destination: song-analysis
          group: songConsumerGrp
          consumer:
            maxAttempts: 1
```

The `maestro_index_requests` topic is for on demand request message instead of using the web api above
the body of the messages should be a JSON, and looks like one of the following:

- Analysis:
```json
{"value" : { "repositoryCode" : "collab", "studyId" : "PEK-AB", "analysisId" : "EGAZ000", "remove": true }	}
```

- Study:
```json
{"value" : { "repositoryCode" : "collab", "studyId" : "PEK-AB" }	}
```

- Full repository (SONG):
```json
{"value" : { "repositoryCode" : "aws" }	}
```

for `song-nalysis` topic messages, the message schemas are governed by SONG but they currently look like this:

```json
{"value" : { "analysisId" : "12314124", "studyId" : "PEK-AB", "songServerId": "collab", "state": "PUBLISHED" }	}
```
