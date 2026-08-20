# Atlas index

- [Deploy-ordering: categoryId/alias routing](roadmap/deploy-ordering-categoryid-alias.md): why Maestro must deploy before Lyric for shared-topic routing, and what happens to messages in between.
- [Slim the published Docker image](roadmap/slim-docker-image.md): removing unneeded build artifacts from the runtime image now that the server runs via `node` directly.
- [Add zod to maestro-provider](roadmap/zod-maestro-provider.md): validating Kafka message inputs, and the version-alignment decision blocking it.
- [Kafka indexing traceability](roadmap/kafka-logging-traceability.md): design for document-ID-level logging across the indexing pipeline, motivated by a real debugging incident.
