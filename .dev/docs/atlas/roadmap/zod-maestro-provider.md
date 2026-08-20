# Add zod to maestro-provider, validate Kafka message inputs

`routeDocumentMessage.ts` extracts categoryId/categoryAlias from a parsed Kafka message with manual `typeof` checks, on genuinely untrusted input, the same class of boundary Lyric validates with zod. `maestro-provider` has no zod dependency today; only `packages/common`/`apps/server` do (`^3.23.8`).

See the tech-debt entry for the broader gap this doesn't close: `parser.ts` still has no shape validation beyond "is it valid JSON."

## Version decision needed first

As of 2026-07-21: latest stable zod overall is `4.4.3`; latest stable `3.x` is `3.25.76` (newer than what's pinned here). Adopting v4 for just this dependency would split the monorepo across two major versions. Options: match existing (`^3.25.76`, no cross-version risk), adopt v4 here only (starts an unplanned partial migration), or upgrade all three at once (its own larger project, check v4's breaking changes against `repositoryConfig.ts`'s schemas first). Re-check the registry before starting, these numbers may be stale by then.

## Scope once a version is chosen

- Add `zod` to `packages/maestro-provider/package.json`
- Schema for the `categoryId`/`categoryAlias` fields `routeDocumentMessage.ts` reads, replacing the `typeof` checks
