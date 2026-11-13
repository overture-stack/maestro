# Migration Guide

Each section covers one change to something you already use: what changed, who is affected, how to tell, and what to do. Sections are grouped by the release you are upgrading from.

## Upgrading from 5.0.x

<a id="deleted-lyric-records"></a>

### Indexing removes the documents of records confirmed deleted by Lyric

For a Lyric repository, Maestro now removes the document of a record once Lyric confirms the record deleted: Lyric answers `404` for the record, and Lyric's audit lists the record's deletion in its organization.

- `POST /index/repository/{repositoryCode}/organization/{organization}/id/{id}` removes the document of such a record and answers `successful: true`, with a `message` saying whether the document was removed or the index held none. Before, it answered `successful: false` with the message `Record '{id}' not found in organization '{organization}'`. If the index fails to remove the document, the route answers `500`, with `serverFault: true` in its body.
- `DELETE` on the same path removes the document of such a record too. It still removes the document while Lyric holds the record.
- `POST /index/repository/{repositoryCode}` and `POST /index/repository/{repositoryCode}/organization/{organization}`, after reading every page of records, also remove each document of the repository, or of the organization, missing from those pages, once Lyric confirms its record deleted.

This affects deployments indexing Lyric repositories, and most of all a caller reading the record route's "not found" answer as a sign that a record was deleted. A reindex logs the outcome as `Removed the documents of deleted records`, counting the documents removed, already gone, kept and failed.

Keep Lyric's audit enabled, since a deletion is confirmed only through it: `features.audit.enabled` in Lyric's configuration, or `AUDIT_ENABLED` for Lyric's reference server, where it is on unless set otherwise. Maestro now reads `GET /audit/category/{categoryId}/organization/{organization}` from Lyric, so allow that route wherever Maestro's access to Lyric is limited to particular routes. After upgrading, reindex each organization indexed from Lyric, or the whole repository, once.

Map `organization` as `keyword` in each index holding Lyric records, since an organization reindex finds the organization's documents through that field. A `text` field with a `keyword` sub-field, as Elasticsearch's dynamic mapping creates, does not meet this. An organization reindex removes the documents of deleted records only from an index meeting this. `GET /<index>/_mapping/field/organization` shows how an index maps the field. Elasticsearch cannot change a field's type in an existing index, so create a new index with the old index's settings and mapping, `organization` changed to `keyword`, and copy the documents into it with Elasticsearch's `_reindex`. Then point the alias at the new index, or, without one, change the index name in Maestro's configuration and in every application reading the index, such as Arranger's. Do this before the reindex after upgrading.

<a id="lyric-listing-errors"></a>

### A reindex reads every page of Lyric records, and stops at one it cannot read

When Lyric answers a page of records with anything but success, the reindex ends and logs an error naming the status, such as `Lyric answered 503 for page 2 of organization 'ABC'`. Before, it ended without a log entry. A first page answered with `404` still means the organization or category holds no records. The pages read before the error stay indexed, and the reindex removes no documents.

This affects anyone watching Maestro's logs for failed reindexes. Nothing needs to change.

Without a configured page size (`MAESTRO_REPOSITORIES_<n>_PAGINATION_SIZE`), a reindex of a Lyric repository now reads every page of records, at Lyric's default of 20 records per page. Before, it read only the first page. This affects deployments leaving the page size unset: they now index every record, and their reindexes run longer. Set a page size to choose how many records each request reads.

<a id="elasticsearch-service"></a>

### `ElasticsearchService` can list documents, and names a document missing from the index

`ElasticsearchService` in `@overture-stack/maestro-common` declares an optional `scanDocuments`, which lists an index's documents with the value of one field, and `@overture-stack/maestro-indexer-client` implements it for Elasticsearch 7 and 8. A reindex removes the documents of deleted records only through an implementation with it. When `deleteData` is asked for a document missing from the index, `failureData[id]` holds `DOCUMENT_NOT_FOUND` (`'not_found'`), and the indexer client logs an error naming the index and the document under the event `maestro.indexer.deleteNotFound`. Before, `failureData[id]` held the name of the error Elasticsearch raised, and the error was logged under the message `Error delete doc`. `successful` is `false` in both cases. This applies to every removal, whatever the repository type: Kafka removals for Lyric and Song, Song `remove` requests, and the record removal route. Anyone alerting on `Error delete doc` for a missing document should match the new event instead.

`ApiResult` also declares an optional `serverFault`, `true` on a failure whose cause lies with Maestro or a service it depends on rather than with the request.

This affects code implementing `ElasticsearchService` itself, code reading the reason given by `deleteData`, and code turning an `ApiResult` into a response. Add `scanDocuments` to an implementation so its reindexes remove the documents of deleted records, compare that reason with `DOCUMENT_NOT_FOUND`, and answer a failure carrying `serverFault` as a server error.
