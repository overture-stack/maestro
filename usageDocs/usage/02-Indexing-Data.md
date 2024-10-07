# Indexing Data

The following sections will demonstrate how to index data at different entity levels using the API. Maestro can index data flexibly at either the repository, study, or individual analysis levels. 

**What is an analysis?** An Analysis file contains the metadata in a structured JSON format. Metadata gets submitted to Song as an Analysis File.

Maestro provides a RESTful API that allows you to interact with its core functionalities. There are two methods available to interact with the Maestro API.

### The Swagger UI

The Swagger UI is a helpful tool for exploration and simple use cases. It provides detailed descriptions of all the available endpoints, expected inputs, and error responses.

![Entity](../assets/swagger.png 'Swagger UI')

Depending on your deployment, you can access the Swagger UI from one of the following links:

| Mode | URL |
| -- | --- |
| Local | `http://localhost:11235/maestro/api-docs` |
| Server | `https://<YOUR-URL>/maestro/api-docs` |

### cURL

Maestro's API can be accessed through the command line using cURLs, this also allows users to create more complex programmatic queries if desired.

Examples and instructions for interacting with Maestro's API will be provided in the following pages.


## Indexing an Analysis

### Using cURL

To index individual analyses with cURL, execute the following from your command line:

```shell
    curl -X POST \
    http://localhost:11235/index/repository/`<repositoryCode>`/study/`<studyId>`/analysis/`<analysisId>` \
    -H 'Content-Type: application/json' \
    -H 'cache-control: no-cache' \
```

Where:

- `repositoryCode` is the code representing the Song repository that the study belongs to
- `studyId` is the ID of the study that the analysis belongs to
- `analysisId` is the ID of the analysis you want to index

### Using Swagger UI

To index a study using the Swagger UI:

1. Go to `http://localhost:11235/maestro/api-docs`

2. Under **management-controller**, select the `POST /index/repository/{repositoryCode}/study/{studyId}/analysis/{analysisId}` endpoint.

3. Click **Try it out**.

4. Enter your `analysisId`, `studyId`, and `repositoryCode`.

7. Click **Execute**.

![Entity](../assets/index-analysis.png 'Index Analysis')

If successful the command line or Swagger will indicate the analysis has been indexed:

```shell
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

## Indexing a Study

The most common way to index is usually by study. This operation will index all analyses in the specific study provided.

### Using cURL

To index a study with cURL, from your command line, execute the following:

```shell
    curl -X POST \
    http://localhost:11235/index/repository/`<repositoryCode>`/study/`<studyId>` \
    -H 'Content-Type: application/json' \
    -H 'cache-control: no-cache' \
```

Where:
- `repositoryCode` is the code representing the Song repository that the study belongs to
- `studyId` is the ID of the study you want to index

### Using Swagger UI

To index a study using the Swagger UI:

1. Go to `http://localhost:11235/maestro/api-docs`

2. Under **management-controller**, click the `POST /index/repository/{repositoryCode}/study/{studyId}` endpoint.

3. Click **Try it out**.

4. Enter your `studyId`, and `repositoryCode`.

6. Click **Execute**. For example:

![Entity](../assets/index-study.png 'Index Study')

If successful the command line or Swagger will indicate the study has been indexed:

```shell
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

## Indexing a Repository


It's possible to index an entire Song repository at once. This operation will index all analyses in all studies within the specified repository.

### Using cURL

To index a repository with cURL, from your command line. execute the following:

```shell
    curl -X POST \
    http://localhost:11235/index/repository/`<repositoryCode>` \
    -H 'Content-Type: application/json' \
    -H 'cache-control: no-cache'
```

Where `repositoryCode` is the code of the Song repository you want to index.

### Using Swagger UI

To index a repository using the Swagger UI:

1. Go to `http://localhost:11235/maestro/api-docs`

2. Under **management-controller**, click the `POST /index/repository/{repositoryCode}` endpoint.

3. Click **Try it out**.

4. Enter the `repositoryCode` of the Song repository you want to index.

5. Click **Execute**. For example:

![Entity](../assets/index-repo2.png 'Index Repo')

If successful the command line or Swagger will indicate the repository has been indexed:

```shell
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