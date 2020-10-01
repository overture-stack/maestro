<h1 align="center">Maestro</h1>
<p align="center">Organize geographically distributed data stored in Song and Score, with a single, configurable index.</p>
<p align="center">
    <a href="https://github.com/overture-stack/maestro">
        <img alt="Under Development" 
            title="Under Development" 
            src="http://www.overture.bio/img/progress-horizontal-RC.svg" width="320" />
    </a>
</p>

[![Documentation Status](https://readthedocs.org/projects/maestro-overture/badge/?version=latest)](https://maestro-overture.readthedocs.io/en/latest/?badge=latest)
[![Slack](http://slack.overture.bio/badge.svg)](http://slack.overture.bio)

## Documentation:
Documentation is hosted on :
- github pages: https://overture-stack.github.io/maestro/
- read the docs: https://maestro-overture.readthedocs.io/en/latest/ (using mkdocs https://docs.readthedocs.io/en/stable/intro/getting-started-with-mkdocs.html)

## Introduction
Meastro was created to enable genomic researchers to enhance their Overture SONGs by building indexes, elastic search
by default, that makes searching Analyses and Studies much more powerful and easier.

## TLDR; 
Skip down to the How to section it has the steps to get started.

### Features:
- Supports indexing from multiple metadata repositories (SONG).
- Multiple indexing requests: analysis, study, full repository.
- Event driven indexing.
    - Integration with SONG to index published analysis and delete suppressed / unpublished analyses
- Ability to Exclude analysis based on different Ids: Study, Analysis, Donor, Sample Or file.
- Slack web hook integration

### Design Goals:
- Reactive
    - Event driven
    - Elastic
    - Resiliency & Fault tolerance
- Failure audit
    - Dead letters queue for faulty messages to be retried later and reviewed.
    - Human readable Error log
- Runtime configurability
- Extendable: separate domain from infrastructure & configuration

## Technologies & libraries:
- Java 11 - OpenJDK 11
- Maven 3 (YOU NEED MAVEN 3.5+, if you don't want to use the wrapper or your IDE points to older version)
- lombok
- Spring boot 2
    - Spring webflux & project reactor
    - Spring retry
    - Spring Cloud
        - Streams (for Kafak integration)
        - config client
    - Spring boot admin + client
- Elasticsearch 7+
- Apache Kafka
- resilience4j:
    - retry module
- vavr 
- Testing libraries:
    - Junit 5
    - Mockito 2
    - testcontainers
    - Spring cloud contract wiremock

## Structure
The project is following the ports/adapters architecture, where the domain is completely isolated from external infrastructure
and frameworks.
- Two maven modules:
    - maestro domain
      - the core features and framework independent logic that is portable and contains the main indexing, rules, notifications
      logic as specified by the business features. Has packages like:
          - entities : contains POJOs and entities
          - api: the logic that fulfills the business features
          - ports: contains the interfaces needed by the api to communicate with anything outside the indexing context.

    - maestro app:
       - The main runnable (spring boot app)
       - Contains the infrastructure and adapters (ports implementations) that is needed to connect the domain
         with the outside world like elastic search, song web clients, configuration files etc.
         It also has the Spring framework configurations here to keep technologies outside of the domain.
         
# Dependencies:
To Successfully run Maestro (as is) you need the following services to be deployed and configure it to use them:
- [Elasticsearch](https://www.elastic.co/products/elasticsearch)
- [Apache Kafka](https://kafka.apache.org/)
- [SONG](https://github.com/overture-stack/SONG)

you can check the sample docker compose files under `./run/docker-compose` for containerized versions of elastic & kafka.
for SONG please check the SONG github repo [here](https://github.com/overture-stack/SONG/tree/develop/dev) 
on how to run it with docker. Or you can run it as jar.

## How to:
- Swagger API access: 
    - localhost:11235/api-docs

Note: if you don't/can't use the Makefile, look inside it for the shell commands and replicate them.
- Compile: `make` 
- Test: `make test`
- Package: `make package`
- Run:
    - Source:
        - Development:
            1. `make docker-start-dev` starts the infrastructure containers
                - kafka
                - elastic search
                - other helper tools if you want like kafka rest proxy
            2. `make run` OR start maestro (Maestro.java) from the IDE/cmd as a java application
    - Docker:
        - Repository: https://hub.docker.com/r/overture/maestro
        - `make docker-start` starts maestro from a docker image along with all needed infrastructure
    - helm:
        - Repository: https://overture-stack.github.io/charts-server/
        - see : https://github.com/overture-stack/helm-charts for instructions and the maestro folder for examples
      
