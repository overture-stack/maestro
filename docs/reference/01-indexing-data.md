# Indexing Data

Maestro offers flexible indexing at the repository, organization, or individual record level, and removal at the record level, alongside a RESTful API for interacting with its core functionality. There are two ways to interact with the Maestro API:

- **The Swagger UI:** The Swagger UI is useful for exploration and simple use cases. It provides detailed descriptions of all available endpoints, expected inputs, and error responses. Depending on your deployment, the Swagger UI can be accessed from the following URLs.
    - Local: `http://localhost:11235/api-docs`
    - Server: `https://<YOUR-URL>/maestro/api-docs`


      :::tip Maestro API Reference Doc
      Checkout the [Maestro API reference](./06-api-reference.mdx) to view this information alongside a templated Maestro Swagger doc.
      :::

- **cURL:** Maestro's API can be accessed through the command line using cURL, allowing for more complex programmatic queries if desired. Templated cURL requests can be found in the Maestro Swagger UI.

:::info Organizations and records
The second path segment is `organization`. Its meaning depends on the repository type. For a Song repository the organization corresponds to a **study**; for a Lyric repository it corresponds to an **organization**. The third segment, the record `id`, corresponds to a Song analysis ID or a Lyric record ID.
:::

## Indexing a Record

A record is a single document: one Song analysis, or one Lyric record.

#### Using cURL

The following is an example of a cURL request for indexing a specific record:

```shell
curl -X POST \
  http://localhost:11235/index/repository/<repositoryCode>/organization/<organization>/id/<id> \
  -H 'Content-Type: application/json' \
  -H 'cache-control: no-cache'
```

Replace `<repositoryCode>`, `<organization>`, and `<id>` with appropriate values.

#### Using Swagger UI

  1. Go to `http://localhost:11235/api-docs`
  2. Under **Indexing**, select:
      - `POST /index/repository/{repositoryCode}/organization/{organization}/id/{id}`
  3. Click **Try it out** and enter your `repositoryCode`, `organization`, and `id`
  4. Click **Execute**

## Indexing an Organization

Indexing by organization is the most common method. This operation indexes all records for the specified organization (for a Song repository, all analyses in the study).

#### Using cURL

```shell
curl -X POST \
  http://localhost:11235/index/repository/<repositoryCode>/organization/<organization> \
  -H 'Content-Type: application/json' \
  -H 'cache-control: no-cache' \
  -d '{}'
```

Replace `<repositoryCode>` and `<organization>` with appropriate values.

#### Using Swagger UI

1. Go to `http://localhost:11235/api-docs`
2. Under **Indexing**, select `POST /index/repository/{repositoryCode}/organization/{organization}`
3. Click **Try it out** and enter your `repositoryCode` and `organization`
4. Click **Execute**

## Indexing a Repository

It is also possible to index an entire repository in one request. This indexes all records across every organization within the specified repository.

#### Using cURL

```shell
curl -X POST \
  http://localhost:11235/index/repository/<repositoryCode> \
  -H 'Content-Type: application/json' \
  -H 'cache-control: no-cache'
```

Replace `<repositoryCode>` with the appropriate value.

#### Using Swagger UI

1. Go to `http://localhost:11235/api-docs`
2. Under **Indexing**, select `POST /index/repository/{repositoryCode}`
3. Click **Try it out**
4. Enter the `repositoryCode` of the repository you want to index
5. Click **Execute**

## Removing a Record from the Index

A single record can be removed from the index using the same path as record-level indexing, with the `DELETE` method. This removes the indexed document only; nothing is deleted from the source Song or Lyric repository.

#### Using cURL

```shell
curl -X DELETE \
  http://localhost:11235/index/repository/<repositoryCode>/organization/<organization>/id/<id> \
  -H 'Content-Type: application/json' \
  -H 'cache-control: no-cache'
```

Replace `<repositoryCode>`, `<organization>`, and `<id>` with appropriate values.

#### Using Swagger UI

1. Go to `http://localhost:11235/api-docs`
2. Under **Indexing**, select `DELETE /index/repository/{repositoryCode}/organization/{organization}/id/{id}`
3. Click **Try it out** and enter your `repositoryCode`, `organization`, and `id`
4. Click **Execute**

## Removing the Documents of Deleted Records

A document stays in the index until Maestro learns that its record was deleted. Maestro treats a record as deleted only once its repository confirms the deletion, so an unreachable or misconfigured repository never empties the index.

| Repository type | Confirms a deletion when | Needs |
|---|---|---|
| Lyric | Two of Lyric's answers agree: Lyric answers `404` for the record, and Lyric's audit lists the record's deletion in its organization | Lyric's audit, below |

- **Indexing a record** removes the record's document instead of indexing it, once its repository confirms the record deleted.
- **Removing a record from the index** removes the record's document while the repository still holds the record, and once the repository confirms its deletion.
- **Indexing an organization or a repository** reads every page of records, then removes each document of that organization, or of the whole repository, missing from those pages, once the repository confirms its record deleted. If any page of records cannot be read, nothing is removed.

Indexing an organization finds its documents through the index's `organization` field, which must be mapped as `keyword`. A `text` field with a `keyword` sub-field, as Elasticsearch's dynamic mapping creates, does not meet this. Indexing an organization removes the documents of deleted records only from an index meeting this.

:::info Lyric's audit
Confirming a deletion needs Lyric's audit: `features.audit.enabled` in Lyric's configuration, or `AUDIT_ENABLED` for Lyric's reference server, where it is on unless set otherwise. Maestro removes the documents of deleted records only while Lyric's audit is on.
:::

## Successful Indexing Response

For all indexing and removal operations, Maestro accepts the request and returns HTTP `202 Accepted` with a body like this:

```json
{
  "successful": true,
  "indexName": "clinical-data-1.0"
}
```

`successful` reports whether the operation was accepted, and `indexName` is the Elasticsearch index the data was written to. For a record confirmed deleted by its repository, a `message` also says whether its document was removed or the index held none. If the index fails to remove that document, Maestro answers `500`.
