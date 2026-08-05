# Maestro

Maestro enables researchers to enhance their Overture Song and Lyric deployments by building powerful search indexes over their metadata. It organizes geographically distributed data from multiple repositories into a single, configurable Elasticsearch index.

</br>

> <div>
> <img align="left" src="ov-logo.png" height="60"/>
> </div>
>
> _Maestro is part of [Overture](https://www.overture.bio/), a collection of open-source software microservices used to create platforms for researchers to organize and share genomics data._

## Documentation

Technical resources for those working with or contributing to the project are available from our official documentation site, the following content can also be read and updated within the `/docs` folder of this repository.

- **[Maestro Overview](https://docs.overture.bio/develop/Maestro/overview)**
- [**Setting up the Development Environment**](https://docs.overture.bio/develop/Maestro/setup)
- [**Reference Docs**](https://docs.overture.bio/develop/Maestro/reference)

## Development Environment

- [Node.js](https://nodejs.org/) v22 or higher
- [pnpm](https://pnpm.io/installation) package manager
- [Docker](https://www.docker.com/) Container platform
- [Elasticsearch](https://www.elastic.co/products/elasticsearch) 7 or higher

Maestro is a TypeScript project managed as a pnpm monorepo. A `Makefile` wraps the common
tasks: `make compile` installs dependencies and builds every package, `make docker-start-dev`
starts the supporting Elasticsearch and Kafka containers, and `make start` runs the server.
Once running, the Swagger UI is available at `http://localhost:11235/api-docs`.

## Support & Contributions

- For support, feature requests, and bug reports, please see our [Support Guide](https://docs.overture.bio/community/support).
- For detailed information on how to contribute to this project, please see our [Contributing Guide](./CONTRIBUTING.md).

## Related Software

The Overture Platform includes the following Overture Components:

</br>

| Software                                                | Description                                                                               |
| ------------------------------------------------------- | ----------------------------------------------------------------------------------------- |
| [Score](https://github.com/overture-stack/score/)       | Transfer data to and from any cloud-based storage system                                  |
| [Song](https://github.com/overture-stack/song/)         | Catalog and manage metadata associated to file data spread across cloud storage systems   |
| [Maestro](https://github.com/overture-stack/maestro/)   | Organizing your distributed data into a centralized Elasticsearch index                   |
| [Arranger](https://github.com/overture-stack/arranger/) | A search API with reusable search UI components                                           |
| [Stage](https://github.com/overture-stack/stage)        | A React-based web portal scaffolding                                                      |
| [Lyric](https://github.com/overture-stack/lyric)        | A model-agnostic, tabular data submission system                                          |
| [Lectern](https://github.com/overture-stack/lectern)    | Schema Manager, designed to validate, store, and manage collections of data dictionaries. |

## Funding Acknowledgement

Overture is supported by grant #U24CA253529 from the National Cancer Institute at the US National Institutes of Health, and additional funding from Genome Canada, the Canada Foundation for Innovation, the Canadian Institutes of Health Research, Canarie, and the Ontario Institute for Cancer Research.
