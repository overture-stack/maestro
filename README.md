# Maestro - Metadata Indexing

[<img hspace="5" src="https://img.shields.io/badge/chat-on--slack-blue?style=for-the-badge">](http://slack.overture.bio)
[<img hspace="5" src="https://img.shields.io/badge/License-gpl--v3.0-blue?style=for-the-badge">](https://github.com/overture-stack/maestro/blob/develop/LICENSE)
[<img hspace="5" src="https://img.shields.io/badge/Code%20of%20Conduct-2.1-blue?style=for-the-badge">](code_of_conduct.md)

<div>
<img align="right" width="120vw" src="icon-maestro.png" alt="maestro-logo"/>
</div>

Maestro organizes metadata dispersed across numerous [Song](https://github.com/overture-stack/song) repositories into a single, searchable Elasticsearch index. This enables upstream search APIs such as [Arranger](https://github.com/overture-stack/arranger) to consume the data and expose it to end users for exploration.

- Built to interact natively with Song and [Arranger](https://github.com/overture-stack/arranger)
- Receives requests through [Kafka](https://kafka.apache.org/) or JSON Web API 
- Index a single analysis, a study or a full Song repository with one request
- Create exclusion rules based off Song metadata tags
- Event-based indexing with apache kafka messaging queue
- Slack integration for convenient notifications

<!--Blockqoute-->

</br>

> 
> <div>
> <img align="left" src="ov-logo.png" height="90"/>
> </div>
> 
> *Maestro is a core component within the [Overture](https://www.overture.bio/) research software ecosystem. Overture is a toolkit of modular software components made to build into scalable data management systems. See our [related products](#related-products) for more information on what Overture can offer.*
> 
> 

<!--Blockqoute-->

## Technical Specifications

- Written in JAVA 
- Indexing for [Elasticsearch 7](https://www.elastic.co/guide/en/elasticsearch/reference/7.17/elasticsearch-intro.html) or up 
- [Swagger UI](https://swagger.io/tools/swagger-ui/) REST API 

## Documentation

- For developer documentation, including instructions for running Ego from source read our [GitHub Wiki](https://github.com/overture-stack/maestro/wiki)
- For user documentation, including installation, configuration and usage guides, see our websites [Maestro documentation](https://www.overture.bio/documentation/maestro/)

## Support & Contributions

- Filing an [issue](https://github.com/overture-stack/maestro/issues)
- Making a [contribution](CONTRIBUTING.md)
- Connect with us on [Slack](https://overture-bio.slack.com/)
- Add or Upvote a [feature request](https://github.com/overture-stack/maestro/issues?q=is%3Aopen+is%3Aissue+label%3Anew-feature+sort%3Areactions-%2B1-desc)

## Related Products 

<div>
  <img align="right" alt="Overture overview" src="https://www.overture.bio/static/124ca0fede460933c64fe4e50465b235/a6d66/system-diagram.png" width="45%" hspace="5">
</div>

Overture is an ecosystem of research software tools, each with narrow responsibilities, designed to address the adapting needs of genomics research. 

All our core microservices are included in the Overture **Data Management System** (DMS). Built from our core collection of microservices, the DMS offers turnkey installation, configuration, and deployment of Overture software. For more information on the DMS, read our [DMS documentation](https://www.overture.bio/documentation/dms/).

See the links below for information on our other research software tools:

</br>

|Software|Description|
|---|---|
|[Ego](https://www.overture.bio/products/ego/)|An authorization and user management service|
|[Ego UI](https://www.overture.bio/products/ego-ui/)|A UI for managing Ego authentication and authorization services|
|[Score](https://www.overture.bio/products/score/)| Transfer data to and from any cloud-based storage system|
|[Song](https://www.overture.bio/products/song/)|Catalog and manage metadata associated to file data spread across cloud storage systems|
|[Maestro](https://www.overture.bio/products/maestro/)|Organizing your distributed data into a centralized Elasticsearch index|
|[Arranger](https://www.overture.bio/products/arranger/)|A search API with reusable UI components that build into configurable and functional data portals|
|[DMS-UI](https://github.com/overture-stack/dms-ui)|A simple web browser UI that integrates Ego and Arranger|