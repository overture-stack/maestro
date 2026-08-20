# Tech debt

## Open

### Kafka message parsing has no shape validation beyond "is it valid JSON"
fix: once zod is added to `maestro-provider` (see `.dev/roadmap.md` "Add zod to maestro-provider"), extend it to the full message shape across all handlers. Needs its own design pass (shape may differ by upstream service/action) and depends on that roadmap item's version decision first, not actionable standalone yet.
standalone: no
context: `parser.ts`'s `parseMessage` only confirms the message is valid JSON; every field read downstream (categoryId/categoryAlias in `routeDocumentMessage.ts`, each handler's own fields) is accessed with ad-hoc checks or trusted outright, on genuinely untrusted input. Pre-existing; the categoryId/categoryAlias work's own reads are narrower, tracked in `.dev/roadmap.md`.

---

## Resolved

<!-- Move entries here when addressed, with a note of when and what fixed it -->
