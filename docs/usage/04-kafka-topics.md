# Kafka Topics

Maestro can be configured to listen to Kafka topics for various operations. This page explains how to set up Kafka integration and use different message types.

## Configuring the Kafka Integration

To enable Kafka integration, add the following configuration to your Maestro application properties or YAML file:

```yaml title="./maestro-app/src/main/resources/config/application.yml"
###############################################################################
# Spring Configuration (Kafka)
# Including Kafka integration with song 
###############################################################################
spring:
  config:
    useLegacyProcessing: true
  application:
    name: maestro
  output.ansi.enabled: ALWAYS
  cloud:
    stream:
      kafka: # remove this key to disable kafka
        binder:
          brokers: localhost:29092
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
        songInput:
          destination: song-analysis
          group: songConsumerGrp
          consumer:
            maxAttempts: 1
        input:
          # We don't specify content type because @StreamListener will handle that
          destination: maestro_index_requests
          group: requestsConsumerGrp
          consumer:
            maxAttempts: 1
```

<details>
<summary><b>For more details about the configuration, click here</b></summary>

**Spring Configuration**
- `spring.config.useLegacyProcessing`: Enables legacy configuration processing mode
- `spring.application.name`: Sets application identifier as "maestro"
- `spring.output.ansi.enabled`: Controls ANSI color output in logs
  - Possible values: `ALWAYS`, `NEVER`, `DETECT`

**Kafka Configuration**
- `spring.cloud.stream.kafka.binder.brokers`: Kafka broker connection URL
- `spring.cloud.stream.kafka.bindings.songInput.consumer`: Song analysis consumer settings
  - `enableDlq`: Enables dead letter queue ([see relevant confluent developer documentation here](https://developer.confluent.io/courses/kafka-connect/error-handling-and-dead-letter-queues/#:~:text=Kafka%20Connect's%20dead%20letter%20queue,at%20their%20keys%20and%20values.))
  - `dlqName`: DLQ name for failed song analysis messages
  - `autoCommitOnError`: Auto-commits offsets on error
  - `autoCommitOffset`: Auto-commits processed message offsets
    - For more information on these configuration see Kafka Consumers docs(https://docs.confluent.io/platform/current/clients/consumer.html)
   - For more details on offset commit behavior, see:
      - [Confluents documenation on Kafka Consumers](https://docs.confluent.io/platform/current/clients/consumer.html)
      - [Spring Cloud Kafka Consumer Properties](https://docs.spring.io/spring-cloud-stream-binder-kafka/docs/current/reference/html/spring-cloud-stream-binder-kafka.html#kafka-consumer-properties)

**Stream Bindings**
 - `spring.cloud.stream.bindings.songInput`:
  - `destination`: Target Kafka topic for song analysis
  - `group`: Consumer group name
  - `maxAttempts`: Maximum retry attempts for message processing
    - Possible values: Integer > 0

- `spring.cloud.stream.bindings.input`:
  - `destination`: Topic for maestro index requests
  - `group`: Consumer group for index requests
  - `maxAttempts`: Maximum processing retry attempts
    - Possible values: Integer > 0
</details>

## Kafka Topics

Maestro listens to two main Kafka topics:

1. `maestro_index_requests`: For on-demand indexing requests
2. `song-analysis`: For Song analysis updates

### maestro_index_requests Topic

This topic is used for sending on-demand indexing requests to Maestro. Messages should be in JSON format and can be of three types:

1. **Analysis Indexing**
   ```json
   {
     "value": {
       "repositoryCode": "collab",
       "studyId": "PEK-AB",
       "analysisId": "EGAZ000",
       "remove": true
     }
   }
   ```
   - `remove`: Set to `true` for deletion, `false` or omit for indexing/updating

2. **Study Indexing**
   ```json
   {
     "value": {
       "repositoryCode": "collab",
       "studyId": "PEK-AB"
     }
   }
   ```

3. **Full Song Repository Indexing**
   ```json
   {
     "value": {
       "repositoryCode": "aws"
     }
   }
   ```

### song-analysis Topic

This topic receives messages about Song analysis updates. The message schema is defined by Song, but typically looks like this:

```json
{
  "value": {
    "analysisId": "12314124",
    "studyId": "PEK-AB",
    "songServerId": "collab",
    "state": "PUBLISHED"
  }
}
```

## Additional Resources

- [Spring Cloud Stream Documentation](https://docs.spring.io/spring-cloud-stream/docs/current/reference/html/)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
