
Maestro can be used through either a message driven kafka topic or an HTTP json API

# Http API

- Index a Song Study:

`POST http://maestro.host:11235/index/repository/<repo>/study/<studyId>`

```bash 
curl -X POST \
	http://localhost:11235/index/repository/collab/study/BASH-AR \
	-H 'Content-Type: application/json' \
	-H 'cache-control: no-cache' \
	-d '{}'
```

- Index an analysis: 

`POST http://maestro.host:11235/index/repository/<repo>/study/<studyId>/analysis/<analysisId>`

```bash
	curl -X POST \
	http://localhost:11235/index/repository/collab/study/BASH-AR/analysis/ad7cabf8-df45-40fe6 \
	-H 'Content-Type: application/json' \
	-H 'cache-control: no-cache'
```

- Index a full repository:

`POST http://maestro.host:11235/index/repository/<repo-code>`

```bash
	curl -X POST \
	http://localhost:11235/index/repository/collab \
	-H 'Content-Type: application/json' \
	-H 'cache-control: no-cache'
```

# Kafka topics

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

- Analysis
```json
{"value" : { "repositoryCode" : "collab", "studyId" : "PEK-AB", "analysisId" : "EGAZ000", "remove": true }	}
```

- Study:
```json
{"value" : { "repositoryCode" : "collab", "studyId" : "PEK-AB" }	}
```

- Full repository (SONG)
```json
{"value" : { "repositoryCode" : "aws" }	}
```

for `song-nalysis` topic messages, the message schemas are governed by SONG but they currently look like this:

```json
{"value" : { "analysisId" : "12314124", "studyId" : "PEK-AB", "songServerId": "collab", "state": "PUBLISHED" }	}
```