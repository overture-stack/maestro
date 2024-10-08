# Setup

## Prerequisites

Before you begin, ensure you have the following installed on your system:
- [JDK11](https://www.oracle.com/ca-en/java/technologies/downloads/)
- [Docker](https://www.docker.com/products/docker-desktop/) (v4.32.0 or higher)

## Developer Setup

This guide will walk you through setting up a complete development environment, including Maestro and its complementary services.

### Setting up supporting services

We'll use our Conductor service, a flexible Docker Compose setup, to spin up Maestro's complementary services.

1. Clone the Conductor repository and move into its directory:

    ```bash
    git clone https://github.com/overture-stack/conductor.git
    cd conductor
    ```

2. Run the appropriate start command for your operating system:

    | Operating System | Command |
    |------------------|---------|
    | Unix/macOS       | `make maestroDev` |
    | Windows          | `make.bat maestroDev` |

    <details>
    <summary>**Click here for a detailed breakdown**</summary>

    This command will set up all complementary services for Score development as follows:

    ![aestroDev](./assets/maestroDev.svg 'Maestro Dev Environment')

    | Service | Port | Description | Purpose in Score Development |
    |---------|------|-------------|------------------------------|
    | Conductor | `9204` | Orchestrates deployments and environment setups | Manages the overall development environment |
    | Keycloak-db | - | Database for Keycloak (no exposed port) | Stores Keycloak data for authentication |
    | Keycloak | `8180` | Authorization and authentication service | Provides OAuth2 authentication for Score |
    | Song-db | `5433` | Database for Song | Stores metadata managed by Song |
    | Song | `8080` | Metadata management service | Manages metadata for files stored by Score |
    | Kafka | `9092` | Distributed event streaming platform | Serves as a messaging queue for publication events used to trigger indexing |
    | Elasticsearch | `9200` | Distributed search and analytics engine | Provides fast and scalable search capabilities over indexed data |

    - Ensure all ports are free on your system before starting the environment.
    - You may need to adjust the ports in the `docker-compose.yml` file if you have conflicts with existing services.

    For more information, see our [Conductor documentation linked here](/docs/other-software/Conductor)

    </details>

### Running the Development Server 

1. Clone Maestro and move into its directory:

    ```bash
    git clone https://github.com/overture-stack/maestro.git
    cd maestro
    ```

2. Build the application locally:

   ```bash
   ./mvnw clean install -DskipTests
   ```

    <details>
    <summary>**Click here for an explaination of command above**</summary>

    - `./mvnw`: This is the Maven wrapper script, which ensures you're using the correct version of Maven.
    - `clean`: This removes any previously compiled files.
    - `install`: This compiles the project, runs tests, and installs the package into your local Maven repository.
    - `-DskipTests`: This flag skips running tests during the build process to speed things up.

    </details>

    :::tip
    Ensure you are running JDK11. To check, you can run `java --version`. You should see something similar to the following:
    ```bash
    openjdk version "11.0.18" 2023-01-17 LTS
    OpenJDK Runtime Environment Corretto-11.0.18.10.1 (build 11.0.18+10-LTS)
    OpenJDK 64-Bit Server VM Corretto-11.0.18.10.1 (build 11.0.18+10-LTS, mixed mode)
    ```
    :::

3. Start the Maestro Server:

   ```bash
    ./mvnw spring-boot:run -pl maestro-app
   ```

    :::info

    If you are looking to configure Maestro for your specific environment, [**Maestro's configuration file can be found here**](https://github.com/overture-stack/maestro/blob/master/maestro-app/src/main/resources/config/application.yml).


    :::

### Verification

After installing and configuring Score, verify that the system is functioning correctly:

1. **Check Server Health**
   ```bash
   curl -s -o /dev/null -w "%{http_code}" "http://localhost:8087/download/ping"
   ```
   - Expected result: Status code `200`
   - Troubleshooting:
     - Ensure Score server is running
     - Check you're using the correct port (default is 8087)
     - Verify no firewall issues are blocking the connection

2. **Check the Swagger UI**
   - Navigate to `http://localhost:8087/swagger-ui.html` in a web browser
   - Expected result: Swagger UI page with a list of available API endpoints
   - Troubleshooting:
     - Check browser console for error messages
     - Verify you're using the correct URL

For further assistance, [open an issue on GitHub](https://github.com/overture-stack/maestro/issues/new?assignees=&labels=&projects=&template=Feature_Requests.md).

:::warning
This guide is meant to demonstrate the configuration and usage of Score for development purposes and is not intended for production. If you ignore this warning and use this in any public or production environment, please remember to use Spring profiles accordingly. For production do not use **dev** profile.
:::







# Old
-------------------------------------------------------------------------------------------------------------------------------------
## Run as a Standalone Server

Provided that you have JDK11+ and all dependencies running and modified `application.yaml` based on your environment and needs, you can run the following command:  

```bash
make run
```
## Run as a Container 

### Prerequisites

To run Maestro you need the following services running:

- [Elasticsearch](https://www.elastic.co/products/elasticsearch) version 7+ to build index in.
- [SONG](https://www.overture.bio/products/song) to use as a metadata source.
- Optional: [Apache Kafka](https://kafka.apache.org/) (if you want event driven integration with song).

In the code repository, configurations are driven by: `config/application.yml`. Change the relevent sections to connect to Elasticsearch, SONG, Kafka based on your setup.

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

In this mode a docker-compose.yaml file will be used, it contains a dockerized version of elasticsearch and kafka see `./run/docker-compose/docker-compose.yaml`. 
For SONG please check the SONG github repo here on how to run it with docker.

- Docker image Repository: [Dockerhub](https://hub.docker.com/r/overture/maestro)

starts maestro from a docker image along with all needed infrastructure

```bash
make docker-start
```


## Kuberenets (Helm)
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
