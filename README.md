# Maestro

Maestro enables researchers to enhance their Overture SONG deployments by building powerful search indexes for Analyses and Studies. It organizes geographically distributed data stored in Song and Score into a single, configurable index.

</br>

> 
> <div>
> <img align="left" src="ov-logo.png" height="60"/>
> </div>
> 
> *Maestro is part of [Overture](https://www.overture.bio/), a collection of open-source software microservices used to create platforms for researchers to organize and share genomics data.*
> 
> 

## Repository Structure

The repository is organized with the following directory structure:

```
.
├── /ci-cd
├── /maestro-app
├── /maestro-domain
```

- __maestro-domain__ - Core features and framework-independent logic that handles indexing, rules, and notifications
- __maestro-app__ - The main Spring Boot application containing infrastructure implementations and framework configurations

## Documentation

Technical resources for those working with or contributing to the project are available from our official documentation site, the following content can also be read and updated within the `/docs` folder of this repository.

- **[Maestro Overview](https://main--overturedocs.netlify.app/docs/core-software/Maestro/overview)** 
- [**Setting up the Development Enviornment**](https://main--overturedocs.netlify.app/docs/core-software/Maestro/setup)
- [**Common Usage Docs**](https://main--overturedocs.netlify.app/docs/core-software/Maestro/setup)

## Development Environment

- [Java 11 (OpenJDK)](https://openjdk.java.net/projects/jdk/11/)
- [Maven 3.5+](https://maven.apache.org/) (or use provided wrapper)
- [VS Code](https://code.visualstudio.com/) or preferred Java IDE
- [Docker](https://www.docker.com/) Container platform

## Support & Contributions

- For support, feature requests, and bug reports, please see our [Support Guide](https://main--overturedocs.netlify.app/community/support).
- For detailed information on how to contribute to this project, please see our [Contributing Guide](https://main--overturedocs.netlify.app/docs/contribution).

## Related Software 

The Overture Platform includes the following Overture Components:

</br>

|Software|Description|
|---|---|
|[Score](https://github.com/overture-stack/score/)| Transfer data to and from any cloud-based storage system |
|[Song](https://github.com/overture-stack/song/)| Catalog and manage metadata associated to file data spread across cloud storage systems |
|[Maestro](https://github.com/overture-stack/maestro/)| Organizing your distributed data into a centralized Elasticsearch index |
|[Arranger](https://github.com/overture-stack/arranger/)| A search API with reusable search UI components |
|[Stage](https://github.com/overture-stack/stage)| A React-based web portal scaffolding |
|[Lyric](https://github.com/overture-stack/lyric)| A model-agnostic, tabular data submission system |
|[Lectern](https://github.com/overture-stack/lectern)| Schema Manager, designed to validate, store, and manage collections of data dictionaries.  |

If you'd like to get started using our platform [check out our quickstart guides](https://main--overturedocs.netlify.app/guides/getting-started)

## Funding Acknowledgement

Overture is supported by grant #U24CA253529 from the National Cancer Institute at the US National Institutes of Health, and additional funding from Genome Canada, the Canada Foundation for Innovation, the Canadian Institutes of Health Research, Canarie, and the Ontario Institute for Cancer Research.
