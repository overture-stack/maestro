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

---

## An `usher-maestro` plugin authorizes Maestro's operations

**Priority:** low
**Status:** not scoped. The goal: Maestro authorizes each indexing and removal operation per principal, through an `usher-maestro` plugin, for a repository, an organization or a record, whether it is requested over HTTP or by a Kafka message. The plugin authorizes operations, unlike `usher-arranger`, which filters query results, and a possible `usher-lyric`, which would gate submissions. No design work has started.

---

## Maestro sends a configured credential with its reads from each repository

**Priority:** medium
**Status:** not scoped. The goal: each repository's configuration can carry a credential, and Maestro sends it with every read from that repository: listing records, reading one record, and reading Lyric's audit to confirm a deletion. Needed before a repository authorizes reads, as iMicroSeq's submission service plans to under imicroseq/roadmap#278. No design work has started.
