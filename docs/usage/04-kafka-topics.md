# Kafka Topics

Maestro can be configured to listen to Kafka topics for various operations. This page explains how to set up Kafka integration and use different message types.

## Configuring the Kafka Integration

To enable Kafka integration, add the following configuration to your Maestro application properties or YAML file:

```yaml
spring:
  application:
    name: maestro
  output.ansi.enabled: ALWAYS
  cloud:
    stream:
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

:::info
To disable Kafka integration, remove the `spring.cloud.stream.kafka` key from your configuration.
:::

## Kafka Topics

Maestro listens to two main Kafka topics:

1. `maestro_index_requests`: For on-demand indexing requests
2. `song-analysis`: For SONG analysis updates

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

3. **Full Repository Indexing (SONG)**
   ```json
   {
     "value": {
       "repositoryCode": "aws"
     }
   }
   ```

### song-analysis Topic

This topic receives messages about SONG analysis updates. The message schema is defined by SONG, but typically looks like this:

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

## Configuration Details

### Kafka Broker
- `spring.cloud.stream.kafka.binder.brokers`: Specifies the Kafka broker address(es)

### Dead Letter Queues (DLQ)
- `enableDlq`: Enables a Dead Letter Queue for failed messages
- `dlqName`: Specifies the name of the DLQ
- `autoCommitOnError`: Automatically commits offset on error
- `autoCommitOffset`: Automatically commits offset after processing

### Consumer Groups
- `group`: Defines the consumer group for each topic
- `maxAttempts`: Sets the maximum number of processing attempts for a message

## Best Practices

1. **Error Handling**: Monitor DLQs regularly to handle failed messages
2. **Message Validation**: Implement proper message validation to ensure correct format and content
3. **Scalability**: Adjust consumer group settings for load balancing across multiple Maestro instances
4. **Monitoring**: Set up monitoring for Kafka topics and consumer groups to track performance and issues

## Troubleshooting

If you encounter issues with Kafka integration:

1. Verify Kafka broker connectivity
2. Check consumer group offsets
3. Inspect DLQs for failed messages
4. Ensure message formats match the expected schemas
5. Review Maestro logs for any Kafka-related errors

:::info
For production deployments, consider implementing additional security measures such as SSL encryption and SASL authentication for Kafka connections.
:::

## Additional Resources

- [Spring Cloud Stream Documentation](https://docs.spring.io/spring-cloud-stream/docs/current/reference/html/)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
