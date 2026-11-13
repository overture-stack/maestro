# Tech debt

## Open

### Kafka message parsing has no shape validation beyond "is it valid JSON"
fix: once zod is added to `maestro-provider` (see `.dev/roadmap.md` "Add zod to maestro-provider"), extend it to the full message shape across all handlers. Needs its own design pass (shape may differ by upstream service/action) and depends on that roadmap item's version decision first, not actionable standalone yet.
standalone: no
context: `parser.ts`'s `parseMessage` only confirms the message is valid JSON; every field read downstream (categoryId/categoryAlias in `routeDocumentMessage.ts`, each handler's own fields) is accessed with ad-hoc checks or trusted outright, on genuinely untrusted input. Pre-existing; the categoryId/categoryAlias work's own reads are narrower, tracked in `.dev/roadmap.md`.

### `maestro-common` declares no `"type"`, so its `.js` output is CommonJS
fix: declare `"type": "module"` in `packages/common/package.json`, per agentics `code-style.md` § Module format. The package is published (`@overture-stack/maestro-common`, 5.0.0 on npm), so the switch changes its entry point from CommonJS to ESM and breaks any external `require()` consumer: ship it as a major version with a migration path, or keep a CommonJS build under a `require` condition in `exports`. Any CommonJS `.js` source file in the package is converted to ESM in the same change; none is tracked today.
standalone: no
context: the only Maestro package without `"type": "module"`; the root and the other three declare it. With no `type` and `module: NodeNext` from `tsconfig.base.json`, `tsc` emits CommonJS into `dist/`, which the ESM workspace consumers (`apps/server`, `maestro-provider`, `indexer-client`) load through ESM's CommonJS interop. Needs a release decision, since a major version of a published package is not a side change.

### Index the compound documents changed by a Lyric commit
fix: add an operation, as an HTTP route and a Kafka message, accepting two lists of Lyric systemIds: documents to reindex and documents to remove. It reindexes the first list and removes the documents in the second once Lyric confirms each record deleted, with `removeDocumentIfRecordDeleted` (`packages/maestro-provider/src/api/reindex.ts`).
standalone: no
context: blocked on Lyric's tech-debt entry "Commit results name every compound document changed by the commit", which supplies the lists. Every document nesting a changed record is reindexed with it, and indexing a record of an entity other than its category's `defaultCentricEntity` indexes the compound documents holding the record, never a document of its own.

### Tests live in `test/` folders rather than beside their source
fix: move each package's specs from `test/` to sit beside the source they cover, as `*.spec.ts`, and drop `test/**/*.spec.ts` from each `.mocharc.json` once none remain. `indexer-client`'s integration harness can stay in `test/integration/`, since its suites share container setup.
standalone: yes
context: the specs for `api.ts`, `reindex.ts`, the Lyric repository and `indexer-client`'s `common/documents.ts` already sit beside their source, and both packages' `.mocharc.json` list `src/**/*.spec.ts` alongside `test/**/*.spec.ts`.

### Spec files are never type-checked
fix: give each package a `tsconfig.test.json` with `noEmit` that includes its specs and `testing/`, and run `tsc -p tsconfig.test.json` with the build or the tests. Then fix what it reports, starting with `packages/maestro-provider/test/repository/lyricRepository.spec.ts`: an import without its `.js` extension, and three `catch` blocks reading `error.message` from an `unknown`.
standalone: yes
context: every `tsconfig.json` excludes `**/*.spec.ts`, and mocha runs specs through tsx, which strips types without checking them. A type error in a spec therefore surfaces only if it also fails at runtime.

### `pnpm lint` fails before linting anything
fix: add `@eslint/js` to the root `devDependencies`, at the version matching the installed `eslint` 9 (check the registry first), then run `pnpm lint` and fix what it reports.
standalone: yes
context: `eslint.config.js` imports `@eslint/js`, which no `package.json` declares, so ESLint stops with `ERR_MODULE_NOT_FOUND` on a fresh install.

### Every document built from a Song analysis is removed when the analysis leaves the indexable states
fix: every path removing a Song analysis's documents removes all of them: the Kafka document handler (`packages/maestro-provider/src/kafka/processMessage/song/handleDocument.ts`), a Kafka request message with `remove` (`kafka/processMessage/requestMessage.ts`), and `removeIndexRecord` (`api/api.ts`) for a Song repository. In analysis-centric mode, each removes the document whose ID is the analysis ID. In file-centric mode, each removes every document holding the analysis ID in `analysis.analysisId`, through a new `ElasticsearchService` operation in the v7 and v8 clients. The operation matches the value exactly, whether the index maps `analysis` as an object or as nested, and returns the number of documents removed.
test: "removes every file document of an analysis that leaves the indexable states" and "removes the document of an analysis that leaves the indexable states" in `handleDocument.spec.ts`, and the same two for a `remove` request and for `removeIndexRecord`; "removes every document holding a value in a field" in the indexer client's integration suite, on Elasticsearch 7 and 8, with `analysis` mapped as an object and as nested.
severity: high: fix before the next release.

### A Song reindex leaves documents only for analyses in the indexable states and the files they list
fix: Maestro's Song listing (`getOrganizationRecords` in `packages/maestro-provider/src/repository/repositories/song/repository.ts`) asks Song for the analyses in the repository's `indexableStudyStates`, through `analysisStates`. With no states configured, it asks for every state. It stops with an error on a page it cannot read, as Maestro's Lyric listings do. `reindex` (`packages/maestro-provider/src/api/reindex.ts`) applies the removal step it applies to Lyric repositories to Song repositories too. A document missing from a complete pass is removed once Song answers that its analysis is outside the indexable states, or, for a file document, that its analysis no longer lists the file. It is kept on any other answer. A file document is matched to its analysis through `analysis.analysisId`. This step also removes the document of a file deleted from an analysis.
test: "removes the documents of an analysis reported by Song as outside the indexable states"; "removes the document of a file no longer listed by its analysis"; "keeps a document when Song gives no state for its analysis"; "keeps every document when Song's listing fails partway"; "reads the analyses in every indexable state".
severity: medium: fix after "Every document built from a Song analysis is removed when the analysis leaves the indexable states".

### A repository answer other than a record or a 404 reaches the caller as a server fault
fix: `getRecord` in the Lyric and Song repositories (`packages/maestro-provider/src/repository/repositories/lyric/repository.ts` and `song/repository.ts`) throws when the repository answers anything but OK or 404, as each already does when the request fails, so the record routes answer 500 through the server's error handler. Lyric's `getRecordStatus` answers a new status, `unavailable`, when either of its two requests fails, when Lyric answers the record with anything but OK or 404, or when it answers the audit with anything but OK. It keeps `unknown` for a 404 with no deletion listed in the audit. `removeIfRecordDeleted` (`packages/maestro-provider/src/api/api.ts`) turns `unavailable` into an answer carrying `serverFault`. A reindex keeps the document on either status, as it does now. `MIGRATION.md` notes the routes' new 500.
standalone: yes
context: the record routes answer 400 "not found" when a repository answers the record request with an error status, and when Lyric's audit request fails or is answered with an error status, so an outage or a credentials problem between Maestro and a repository reads as missing records.

### Maestro takes no `.` or `..` as an organization or record ID
fix: `organizationSchema` and `recordIdSchema` in `apps/server/src/utils/requestSchemas.ts` refuse `.` and `..` after trimming, so the routes answer 400 before reading a repository. The Lyric and Song repositories (`packages/maestro-provider/src/repository/repositories/lyric/repository.ts` and `song/repository.ts`) refuse the same values for every other caller, Kafka messages included, and build each request path with `path.posix.join` from segments each passed through `encodeURIComponent`.
test: "refuses an organization of `.`, `..` or ` .. `" and "refuses a record ID of `.`, `..` or ` .. `" for each route taking one; "refuses an organization or record ID of `.` or `..`" for each read of the Lyric and Song repositories; "builds each request path from encoded segments joined by forward slashes".
severity: medium: fix before Maestro #299 ships.

### An organization reindex warns, and removes nothing, when the index does not map `organization` as `keyword`
fix: before listing an organization's documents, `removeDeletedDocuments` (`packages/maestro-provider/src/api/reindex.ts`) reads how the index maps `organization`, through a new optional `ElasticsearchService` operation implemented in the v7 and v8 clients. For any type other than `keyword`, including a field missing from the index's mapping, it logs a warning naming the index and the type found, under the event `maestro.reindex.removal`, and removes nothing. Behind an alias, each index named by the alias must map the field as `keyword`, and one without the field counts as any other type. An indexer without the operation is logged and skipped the same way, never assumed to map the field as `keyword`.
standalone: yes
context: `docs/reference/01-indexing-data.md` § Removing the Documents of Deleted Records states the requirement. This check makes a reindex report an index not meeting it.

---

## Resolved

<!-- Move entries here when addressed, with a note of when and what fixed it -->
