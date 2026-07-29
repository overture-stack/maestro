# Indexing Data

Maestro offers flexible indexing at the repository, organization, or individual record level, alongside a RESTful API for interacting with its core functionality. There are two ways to interact with the Maestro API:

- **The Swagger UI:** The Swagger UI is useful for exploration and simple use cases. It provides detailed descriptions of all available endpoints, expected inputs, and error responses. Depending on your deployment, the Swagger UI can be accessed from the following URLs.
    - Local: `http://localhost:11235/api-docs`
    - Server: `https://<YOUR-URL>/maestro/api-docs`


      :::tip Maestro API Reference Doc
      Checkout the [Maestro API reference](/develop/Maestro/reference/api-reference) to view this information alongside a templated Maestro Swagger doc.
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

## Successful Indexing Response

For all indexing operations, Maestro accepts the request and returns HTTP `202 Accepted` with a body like this:

```json
{
  "successful": true,
  "indexName": "clinical-data-1.0"
}
```

`successful` reports whether the operation was accepted, and `indexName` is the Elasticsearch index the data was written to.
