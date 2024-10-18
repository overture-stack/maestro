# Index Mappings

An index mapping defines how documents and their fields are stored and indexed in Elasticsearch. Maestro is responsible for taking published Song metadata and translating it into Elasticsearch documents.

Depending on how Maestro is configured it can index data into documents in one of two ways:

- **File Centric Indexing:** Each document indexed in Elasticsearch describes all information central to a specific file. [Click here to see an example of a file centric JSON document](https://github.com/overture-stack/composer/blob/develop/guideMaterials/dataAdministration/ES-fileCentric-document.json).

- **Analysis Centric Indexing** Each document indexed in Elasticsearch describes all information central to a specific analysis. [Click here to see an example of an analysis centric JSON document](https://github.com/overture-stack/composer/blob/develop/guideMaterials/dataAdministration/ES-analysisCentric-document.json).

    :::info File or Analysis Centric Indexing
    If your queries focus on individual files and their attributes, choose file-centric indexing. If your queries center on analyses/participants and their associated data, choose analysis-centric indexing.
    :::


The index mapping needs conform with your specific data model and therefore should be configured appropriatly. For more information see our [**platform guide covering index mappings**](/guides/administration-guides/index-mappings). 

While index mapping are not configurable at runtime, there may be cases where an administrator needs to change the mapping. For example, when updating Song's dynamic schemas, the administrator must also update the index mapping.

:::tip Feature Request In development
As part of our [**new data submission system**](/docs/under-development/) we are working towards having Maestro automatically generate these index mappings based on a provided schema
:::

<!--- For PR, is the description below accurate, is it detailed enough? --->

## Guidelines for Index Migration

Follow these steps to migrate your index when changes to the mapping are required:

1. **Update the existing index mapping:** Modify the [index mapping file](https://github.com/overture-stack/maestro/blob/master/maestro-app/src/main/resources/file_centric.json) to account for new analysis types and fields.

2. **Re-index the data:** Trigger indexing by updating the `.env.maestro` environment file with the new index mapping or index via the Elasticsearch API.

3. **Update Elasticsearch aliases:** Point your Elasticsearch aliases to the new or updated index instead of the old one.

:::tip Index Mapping Guide
For more detailed information see our [**platform guide on index mappings**](/guides/administration-guides/index-mappings)
:::

## Best Practices

- Always test your new mapping in a non-production environment before applying changes to production.
- Keep a backup of your old index until you've verified that the new index is working correctly.
- Document all changes made to the index mapping for future reference.

    :::caution
    Changing index mappings can have significant impacts on how your data is stored and queried (Ex. Arranger Search API and UI configurations). Always ensure you understand the implications of any changes before implementing them. 
    :::

