# Index Mappings

An index mapping defines how documents and their fields are stored and indexed in Elasticsearch. Maestro is responsible for taking data from its configured repositories and translating it into Elasticsearch documents.

For Song repositories, Maestro can index data into documents in one of two ways:

- **File Centric Indexing:** Each document indexed in Elasticsearch describes all information central to a specific file. [Click here to see an example of a file centric JSON document](https://github.com/overture-stack/composer/blob/develop/guideMaterials/dataAdministration/ES-fileCentric-document.json).

- **Analysis Centric Indexing:** Each document indexed in Elasticsearch describes all information central to a specific analysis. [Click here to see an example of an analysis centric JSON document](https://github.com/overture-stack/composer/blob/develop/guideMaterials/dataAdministration/ES-analysisCentric-document.json).

    :::info File or Analysis Centric Indexing
    If your queries focus on individual files and their attributes, choose file-centric indexing. If your queries center on analyses/participants and their associated data, choose analysis-centric indexing.
    :::

You select the mode per Song repository through configuration, using the `MAESTRO_REPOSITORIES_<n>_SONG_INDEXING_MODE` environment variable, which accepts `file` or `analysis`:

```bash
# Index repository 1 as file-centric documents
MAESTRO_REPOSITORIES_1_SONG_INDEXING_MODE=file
```

The index mapping needs to conform with your specific data model, so it should be configured appropriately. For more information, see our [**platform guide covering index mappings**](/use/administration/index-mappings).

Maestro works with a dynamic schema. It only requires the base fields of a record, and it passes along any additional fields the record carries, relying on Elasticsearch dynamic mapping for those. Evolving the mapping when your data model changes is the administrator's responsibility. For example, when updating Song's dynamic schemas, the administrator may also need to update the index mapping and migrate the data.

## Guidelines for Index Migration

Follow these steps to migrate your index when changes to the mapping are required:

1. **Create a new index with the updated mapping:** Define an index whose mapping accounts for the new analysis types and fields.

2. **Re-index the data:** Point Maestro at the new index through the repository's `MAESTRO_REPOSITORIES_<n>_INDEX_NAME` configuration and trigger indexing on the full repository, or use the Elasticsearch [`/_reindex`](https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-reindex.html) API.

3. **Update Elasticsearch aliases:** Point your Elasticsearch aliases to the new or updated index instead of the old one.

:::tip Index Mapping Guide
For more detailed information see our [**platform guide on index mappings**](/use/administration/index-mappings)
:::

## Best Practices

- Always test your new mapping in a non-production environment before applying changes to production.
- Keep a backup of your old index until you've verified that the new index is working correctly.
- Document all changes made to the index mapping for future reference.

    :::caution
    Changing index mappings can have significant impacts on how your data is stored and queried (Ex. Arranger Search API and UI configurations). Always ensure you understand the implications of any changes before implementing them.
    :::
