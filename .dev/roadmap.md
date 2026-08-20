# Maestro Roadmap

## Deploy-ordering gap: categoryId/alias routing requires Lyric to ship its side too

**Priority:** low (operational note, not a code fix)
**Status:** documented constraint, not a bug. Maestro must deploy before Lyric for shared-topic categoryId/alias routing to work correctly. Depth: [atlas/roadmap/deploy-ordering-categoryid-alias.md](docs/atlas/roadmap/deploy-ordering-categoryid-alias.md)

---

## Slim the published Docker image

**Priority:** low
**Status:** not started. Runtime image copies more than it needs now that the server runs via `node` directly (no pnpm). Depth: [atlas/roadmap/slim-docker-image.md](docs/atlas/roadmap/slim-docker-image.md)

---

## Add zod to maestro-provider, validate Kafka message inputs

**Priority:** medium
**Status:** blocked on a version decision (align with existing `^3.x` vs. adopt `v4`). Depth: [atlas/roadmap/zod-maestro-provider.md](docs/atlas/roadmap/zod-maestro-provider.md)

---

## Logging improvements: traceability for Kafka indexing

**Priority:** medium
**Status:** design written up, not yet implemented. Motivated by a real incident where a bulk upsert's success couldn't be traced to specific documents. Depth: [atlas/roadmap/kafka-logging-traceability.md](docs/atlas/roadmap/kafka-logging-traceability.md)
