# Indexing Data

Maestro offers flexible indexing at the repository, study, or individual document (analysis file) levels alongside a RESTful API for interacting with its core functionalities. There are two methods to interact with the Maestro API:

- **The Swagger UI:** The Swagger UI is useful for exploration and simple use cases. It provides detailed descriptions of all available endpoints, expected inputs, and error responses. Depending on your deployment the swagger UI can be accessed from the following URLs.
    - Local: `http://localhost:11235/maestro/api-docs`
    - Server: `https://<YOUR-URL>/maestro/api-docs`


      :::tip Maestro API Reference Doc
      Checkout the [Maestro API reference](/docs/core-software/Maestro/usage/api-reference) to view this information alongside a templated Maestro swagger doc.
      :::

- **cURL:** Maestro's API can be accessed through the command line using cURL, allowing for more complex programmatic queries if desired. Templated cURL requests can be found from the Maestro Swagger UI 

## Indexing an Analysis

#### Using cURL

The following is an example of a cURL request for indexing a specfic analysisId:

```shell
curl -X POST \
  http://localhost:11235/index/repository/<repositoryCode>/study/<studyId>/analysis/<analysisId> \
  -H 'Content-Type: application/json' \
  -H 'cache-control: no-cache'
```

Replace `<repositoryCode>`, `<studyId>`, and `<analysisId>` with appropriate values.

#### Using Swagger UI

  1. Go to `http://localhost:11235/maestro/api-docs`
  2. Under **management-controller**, select: 
      - `POST /index/repository/{repositoryCode}/study/{studyId}/analysis/{analysisId}`
  3. Click **Try it out** & enter your `analysisId`, `studyId`, and `repositoryCode`
  5. Click **Execute**

  ![Entity](../assets/index-analysis.png 'Index Analysis')

## Indexing a Study

Indexing by study is the most common method. This operation indexes all analyses in the specified study.

:::info
A study is an organized collection of analysis files. Analysis files are always tagged with a study_ID, which allows them to be grouped and managed together.
:::

#### Using cURL

```shell
curl -X POST \
  http://localhost:11235/index/repository/<repositoryCode>/study/<studyId> \
  -H 'Content-Type: application/json' \
  -H 'cache-control: no-cache'
```

Replace `<repositoryCode>` and `<studyId>` with appropriate values.

#### Using Swagger UI

The following is an example of a Swagger request for indexing a specfic studyId

1. Go to `http://localhost:11235/maestro/api-docs`
2. Under **management-controller**, select `POST /index/repository/{repositoryCode}/study/{studyId}`
3. Click **Try it out** & enter your `studyId` and `repositoryCode`
5. Click **Execute**

![Entity](../assets/index-study.png 'Index Study')

## Indexing a Repository

It is also possible to index an entire Song repository in one request, this will index all analyses in all studies within the specified repository.

#### Using cURL

```shell
curl -X POST \
  http://localhost:11235/index/repository/<repositoryCode> \
  -H 'Content-Type: application/json' \
  -H 'cache-control: no-cache'
```

Replace `<repositoryCode>` with the appropriate value.

#### Using Swagger UI

1. Go to `http://localhost:11235/maestro/api-docs`
2. Under **management-controller**, select `POST /index/repository/{repositoryCode}`
3. Click **Try it out**
4. Enter the `repositoryCode` of the Song repository you want to index
5. Click **Execute**

  ![Entity](../assets/index-repo2.png 'Index Repo')

## Successful Indexing Response

For all indexing operations, a successful response will look like this:

```json
[
  {
    "indexName": "file_centric_1",
    "failureData": {
      "failingIds": {}
    },
    "successful": true
  }
]
```

This response indicates that the indexing operation was successful and no failures were encountered.