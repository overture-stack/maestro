# Deploy-ordering gap: categoryId/alias routing requires Lyric to ship its side too

Fixing wrong-index routing on a shared Kafka topic required both sides: Maestro now matches by `categoryId`/`categoryAlias` for every Lyric repo, regardless of how many share a topic (`repositoryUtils.ts`, `routeDocumentMessage.ts`); Lyric now emits those fields.

Agreed order: Maestro first, then Lyric. Between deploys, a message with no `categoryId` (old Lyric) doesn't match, and is logged and skipped, not DLQ'd: deliberately no fallback, since Kafka publishing in Lyric is new and there's no legacy stream to support.

Applies to every Lyric repo, not just topics shared by more than one. Not a bug: keep the two deploys close together, or expect a brief indexing gap in between.
