# Introduction

Maestro was created to enable genomic researchers to enhance their Overture [SONG](https://www.overture.bio/products/song)
by building search indexes, Elasticsearch by default, that makes searching Analyses and Studies much more powerful and easier.
Created indexes are then easy to integrate with [Arranger](https://www.overture.bio/products/arranger)



# Features

###  Multiple SONGs One Index
Supports indexing from multiple metadata repositories [SONG](https://www.overture.bio/products/song).
Maestro can be connected to multiple SONGs and it will index all files in one elasticsearch index, and if the same file was identified in multiple SONGs (In case of GEO Replication) it will aggregate all repositories information in the same index document.

example of how the document will be : 

``` json

   {
      "object_id": "9ce9358d-c93a-5f83-8032-4addcb84b51a",
      "access": "controlled",
      "study": "PEME-CA",
      "analysis": {
        "id": "EGAZ00001254247",
        "type": "sequencingRead",
        "state": "PUBLISHED",
        "study": "PEME-CA",
        "experiment": {
          "analysisId": "EGAZ00001254247",
          "aligned": true,
          "libraryStrategy": "WGS"
        }
      },
      "file": {
        "name": "b2e40ebb719e1754e99be2e752239639.control_MDT-AP-0432_merged.mdup.bam",
        "format": "BAM",
        "md5sum": "b2e40ebb719e1754e99be2e752239639",
        "size": 77675501639,
        "last_modified": null,
        "index_file": {
          "object_id": "6aa8d318-e168-520e-9d1e-cef127ee6b65",
          "name": "b2e40ebb719e1754e99be2e752239639.control_MDT-AP-0432_merged.mdup.bam.bai",
          "format": "BAI",
          "md5sum": "227a2a11d660a8a53a7375c6623034e2",
          "size": 8982928
        }
      },
      "repositories": [
        {
          "code": "collab",
          "organization": "ICGC",
          "name": "collaboratory",
          "type": "S3",
          "country": "CA",
          "base_url": "##COLLAB_REPO_URL##",
          "data_path": "/oicr.icgc/data",
          "metadata_path": "/oicr.icgc.meta/metadata/464116e4-afc9-5879-b567-6f54513a32dc"
        },
        {
          "code": "aws",
          "organization": "ICGC",
          "name": "aws repo",
          "type": "S3",
          "country": "US",
          "base_url": "##AWS_REPO_URL##",
          "data_path": "/oicr.icgc/data",
          "metadata_path": "/oicr.icgc.meta/metadata/464116e4-afc9-5879-b567-6f54513a32dc"
        }
      ],
      "donors": [
        {
          "id": "DO232959",
          "submitted_id": "MDT-AP-0432",
          "specimen": {
            "id": "SP200947",
            "type": "Normal - blood derived",
            "submitted_id": "MDT-AP-0432_control_specimen",
            "sample": {
              "id": "SA604905",
              "submitted_id": "MDT-AP-0432_control",
              "type": "DNA"
            }
          }
        }
      ]
    }

```

### Multiple indexing levels 
Maestro can index different metadata entitites at once: Analysis, Study or full Repository.

### Different indexing APIs
- Event driven indexing: Kafka integration with SONG to index published analysis and delete suppressed / unpublished analyses, 
(see the [Configurations](#configurations) Kafka settings)
- HTTP json API see [Using Maestro](#using-maestro)

### Ability to Exclude
You can configure the installation to exclude specific analyses by one of the following Ids: Study, Analysis, Donor, Sample Or file. (see the [Configurations](#configurations) exclusion rules section )

### Slack integration
You can configure Maestro to send specific notifications to a slack webhook integration 
currently notifications are for errors during the indexing process. 


# Running Maestro
Source Code: [Github](https://github.com/overture-stack/maestro)

## Dependencies
To run Maestro you need the following services running:

- [Elasticsearch](https://www.elastic.co/products/elasticsearch) version 7+ to build index in.
- [SONG](https://www.overture.bio/products/song) SONG to use as source for metadata.
- Optional: [Apache Kafka](https://kafka.apache.org/) (if you want event driven integration with song).


## Configurations

in the code repository the configurations are in this file: `config/application.yml`.
you will need to change the relevent sections to connect to elasticsearch, song, kafka based on
your setup.

```yaml
server:
  port: 11235

maestro:
  song:
    maxRetries: 3
    timeoutSec:
        study: 100 # some studies take really long, +30 secs, to be downloaded
        analysis: 5

  # elastic search server to connect to & client properties
  elasticsearch:
    # elasticsearch server nodes to send requests to
    clusterNodes:
      - http://localhost:9200
      - http://localhost:9201

    # the index name to store documents in (will be created if not existing)
    indexes:
      fileCentric:
          name: file_centric
          alias: file_centric

    # elasticsearch client properties
    client:
      # this is to control the number of documents per bulk request in elasticsearch
      docsPerBulkReqMax: 5000
      # max time to wait for a connection to be established
      connectionTimeout: 5000
      # max time to wait on idle connection (no data flow)
      socketTimeout: 10000
      # in case of failure this controls the retry attempts
      retry:
        # maximum number of retry attempts before throwing an error
        maxAttempts: 3
        # waiting between retries (ms)
        waitDurationMillis: 500

  # List of Genomic files repositories (SONGs)
  repositories:
    # these properties will be used in the document (see ../file_centric.json)
    - code: song.overture # must be unique & must match song.serverId if using kafka integration with song
      url: "http://localhost:8080"
      name: local song
      dataPath: /oicr.icgc/data
      metadataPath: /oicr.icgc.meta/metadata
      # optional
      storageType: S3
      organization: ICGC
      country: CA
    # you can other SONGs as needed
    - code: song.overture
      url: "http://localhost:8080"
      name: local song
      metadataPath: /oicr.icgc.meta/metadata
      # optional
      storageType: S3
      organization: overture
      country: LH

  # last resort fallback file system log in case of retries exhaustion.
  failureLog:
    enabled: true
    dir: ${user.home}/logs/maestro

  notifications:
    slack:
      # enable/disable slack notifications
      enabled: false
      # the types to trigger a notification to this channel (see NotificationName.java)
      notifiedOn:
        - ALL
      # slack workspace url
      url: https://hooks.slack.com/services/SECRET_TOKEN
      channel: maestro-alerts
      username: maestro
      maxDataLength: 1000
      # notifications has two parameters (TYPE [string], DATA[map])
      templates:
        error: ':bangbang: Error : ##TYPE##, Error Info: ```##DATA##```'
        warning: ':warning: ##TYPE## ```##DATA##```'
        info: ':information_source: ##TYPE## ```##DATA##```'

  # exclusion rules configs
  exclusionRules:
    byId:
      study:
        - "test123"
#      analysis:
#        - "analysisId"
#      file:
#        - 41ba4fb3-9428-50b5-af6c-d779cd59b04d
#      sample:
#        - "sampleId"
#      specimen:
#        - "specimenId"
#      donor:
#        - DO232991

# logging & monitoring
logging:
  level:
    root: INFO
    bio.overture: TRACE
    # very verbose class, only enable lower level when necessary
    bio.overture.maestro.domain.entities.indexing.rules.IDExclusionRule: INFO
    org.apache.kafka.clients: INFO

# spring boot actuator endpoints
management:
  endpoints:
    web:
      exposure:
        include: '*'
  endpoint:
    health:
      show_details: ALWAYS

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



## Running Locally

Maestro has a `Makefile` for convenience if you can't use make you can check the make file for
the commands.

### Source Code (No Docker)
Provided that you have JDK11+ and all dependencies (see [Dependencies](#dependencies)) running and modified `application.yaml` based on your environment and needs, you can run the following command:  

```bash
make run
```

### Docker (Recommended for Local installations)
In this mode a docker-compose.yaml file will be used, it contains a dockerized version of elasticsearch and kafka see `./run/docker-compose/docker-compose.yaml`. 
For SONG please check the SONG github repo here on how to run it with docker.

- Docker image Repository: [Dockerhub](https://hub.docker.com/r/overture/maestro)

starts maestro from a docker image along with all needed infrastructure

```bash
make docker-start
```

### Kuberenets (Helm)
if you want to run in a Kubernetes cluster you can use the maestro helm chart

- [Chart Repository](https://overture-stack.github.io/charts-server/)

prepare your `values-override.yaml` file based on your env, you can provide the 
app configs as env variables using the extraEnv key:

```yaml
extraEnv:
  SERVER_PORT: "11235"
  MAESTRO_ELASTICSEARCH_CLUSTERNODES_0: "http://localhost:9200"
  SPRING_CLOUD_STREAM_KAFKA_BINDER_BROKERS: "localhost:9092"
  # repos
  MAESTRO_REPOSITORIES_0_CODE: "song"
  MAESTRO_REPOSITORIES_0_URL: "https://song1:8080"
  MAESTRO_REPOSITORIES_0_NAME: "song1"
  MAESTRO_REPOSITORIES_0_ORGANIZATION: "ICGC"
  MAESTRO_REPOSITORIES_0_COUNTRY: "CA"
  MAESTRO_REPOSITORIES_1_CODE: "song2"
  MAESTRO_REPOSITORIES_1_URL: "http://song2:8080"
  MAESTRO_REPOSITORIES_1_NAME: "song2"
  MAESTRO_REPOSITORIES_1_ORGANIZATION: "overture"
  MAESTRO_REPOSITORIES_1_COUNTRY: "OICR"
  MAESTRO_FAILURELOG_DIR: "/app-log"
  # slack
  MAESTRO_NOTIFICATIONS_SLACK_ENABLED: "true"
  MAESTRO_NOTIFICATIONS_SLACK_URL: "secret"
  MAESTRO_NOTIFICATIONS_SLACK_CHANNEL: "maestro-argo-notif"
```

then add overture chart repository and install the chart:

```bash
helm repo add overture https://overture-stack.github.io/charts-server/
helm install -f values-override.yaml overture/maestro
```



# Using Maestro

Maestro can be used through either a message driven kafka topic or an HTTP json API

## Http API

- Index a Song Study:

`POST http://maestro.host:11235/index/repository/<repo>/study/<studyId>`

```bash 
curl -X POST \
	http://localhost:11235/index/repository/collab/study/PACA-CA \
	-H 'Content-Type: application/json' \
	-H 'cache-control: no-cache' \
	-d '{}'
```

- Index an analysis: 

`POST http://maestro.host:11235/index/repository/<repo>/study/<studyId>/analysis/<analysisId>`

```bash
	curl -X POST \
	http://localhost:11235/index/repository/collab/study/PACA-CA/analysis/ad7cabf8-df45-40f8-9fcb-67d8933f46e6 \
	-H 'Content-Type: application/json' \
	-H 'cache-control: no-cache'
```

- Index a full repository:

`POST http://maestro.host:11235/index/repository/<repo>`

```bash
	curl -X POST \
	http://localhost:11235/index/repository/collab \
	-H 'Content-Type: application/json' \
	-H 'cache-control: no-cache'
```
# Technical Documentation

## Technical Design Goals
- Reactive
    - Event driven
    - Elastic
    - Resiliency & Fault tolerance
- Failure audit
       - Dead letters queue for faulty messages to be retried later and reviewed.
       - Human readable Error log
- Extendable: 
    separate domain from infrastructure & configuration

## Technologies & libraries
- Java 11 - OpenJDK 11
- Maven 3 (YOU NEED MAVEN 3.5+, if you don't want to use the wrapper or your IDE points to older version)
- lombok
- Spring boot 2
    - Spring webflux & project reactor
    - Spring retry
    - Spring Cloud
        - Streams (for Kafak integration)
        - config client
    - Spring boot admin client
- Elasticsearch 7+
- Apache Kafka
- resilience4j:
    - retry module
- vavr
- Testing libraries:
    - Junit 5
    - Mockito 2
    - testcontainers
    - Spring cloud contract wiremock

## Code Structure 
The project is following the ports/adapters architecture, where the domain is completely isolated from external infrastructure and frameworks.

Two maven modules:

- Maestro domain: 
    - The core features and framework independent logic that is portable and contains the main indexing, rules, notifications logic as specified by the business features. Has packages like:
        - entities : contains POJOs and entities
        - api: the logic that fulfills the business features
        - ports: contains the interfaces needed by the api to communicate with anything outside the indexing context.

- Maestro app:
    - The main runnable (spring boot app)
    - Contains the infrastructure and adapters (ports implementations) that is needed to connect the domain with the outside world like elastic search, song web clients, configuration files etc. It also has the Spring framework configurations here to keep technologies outside of the domain.



# License

Copyright (c) 2018. Ontario Institute for Cancer Research

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see https://www.gnu.org/licenses.
