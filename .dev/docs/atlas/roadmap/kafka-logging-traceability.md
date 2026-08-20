# Logging improvements: traceability for Kafka indexing

During a debugging session in the dev VirusSeq environment (2026-06-23), it took significant effort to determine whether 167 bulk upserts reported as successful by maestro had actually written the expected field (`analysis.lineage_analysis`) into the ES documents. The logs confirmed 200 responses but provided no way to trace which documents were indexed or what was in them.

## Current state

- `consumer.ts` logs the topic name but no document identity (no source-provided ID, no index target)
- Per-upstream `handleDocument.ts` handlers log the repository name but nothing that identifies the specific document
- `operations.ts` `bulkUpsert` logs document count and HTTP status but no document IDs and no field presence information
- Per-item failures in `bulkUpsert` are tracked by array index (`indexItem`), not by document ID: impossible to know which document failed
- No end-of-batch summary
- `debug` level exists but no env var to enable it is surfaced

## Proposed changes

### 1. Log document identity at the point of each upstream handler

Each `handleDocument.ts` (one per upstream service, e.g. `song/handleDocument.ts`, `lyric/handleDocument.ts`) should log the document's source-provided identity at the start of the indexable-state branch:

```ts
logger.info(`Indexing document`, { source: 'song', documentId: payload.analysisId, state: payload.state });
```

Use whatever ID the upstream service provides. The `source` field makes it possible to filter logs by upstream service without parsing topic names.

### 2. Log document IDs going into bulkUpsert at debug level

File: `packages/indexer-client/src/client/v8/operations.ts`

Before the `client.bulk(...)` call:

```ts
logger.debug(
  `Bulk upsert: ${dataSet.length} document(s) in '${index}'`,
  { ids: dataSet.map(d => d._id) }
);
```

At debug level to avoid noise in steady-state operations, but enables precise tracing when `LOG_LEVEL=debug`.

### 3. Log per-item failures with document ID

Same file, in the failure-detection loop after the bulk response:

```ts
// current: failureData[indexItem] = [operation.error?.reason || 'unspecified error'];
// change to:
const docId = dataSet[indexItem]?._id ?? `index:${indexItem}`;
failureData[docId] = [operation.error?.reason || 'unspecified error'];
logger.error(`Bulk upsert item failed`, { index, docId, reason: operation.error?.reason });
```

Failure logs will now include the document ID instead of the positional index, which is actionable.

### 4. Add a per-message outcome summary line

Each `handleDocument.ts` handler should close each document log with an outcome after `indexer.bulkUpsert(...)` returns:

```ts
logger.info(`Document indexed`, { source: 'song', documentId: payload.analysisId, index: indexName, successful: result.successful });
```

Makes it trivial to grep for failures: `grep '"successful":false'`.

### 5. Surface LOG_LEVEL in the env schema and README

File: `apps/server/.env.example` (or equivalent)

```
LOG_LEVEL=info    # set to 'debug' to enable verbose document-level tracing
```

Document in `docs/setup.md` that `LOG_LEVEL=debug` enables document ID and field-level tracing via the debug calls above.

## Acceptance criteria

- Each processed Kafka message produces at least one log line containing the source-provided document ID
- A failed bulk item log contains the document ID, not its positional index
- Running with `LOG_LEVEL=debug` produces a log line listing the IDs sent to each bulk upsert
- Applies consistently across all upstream service handlers, not just one
- An end-to-end test or manual smoke test can confirm the above with a real Kafka event
