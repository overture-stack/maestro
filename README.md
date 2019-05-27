# Maestro

The indexer component of genomic studies metadata.

## Intro
Meastro was created to enable genomic researchers to enhance their Overture SONGs by building indexes, elastic search
by default, that makes searching Analyses and Studies much more powerful and easier.


### Features:
- Supports indexing from multiple metadata repositories (SONG).
- Multiple indexing requests: analysis, study, full repository.
- Event driven indexing.
- Ability to Exclude analysis based on different Ids: Study, Analysis, Donor, Sample Or file.

### Design Goals:
- Reactive
    - Event driven
    - Elastic
    - Resiliency & Fault tolerance
- Failure audit
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

    - mastro app:
       - The main runnable (spring boot app)
       - Contains the infrastructure and adapters (ports implementations) that is needed to connect the domain
         with the outside world like elastic search, song web clients, configuration files etc.
         It also has the Spring framework configurations here to keep technologies outside of the domain.
# Dependencies:
To Successfully run Maestro (as is) you need the following services to be deployed and configure it to use them:
- [Elasticsearch](https://www.elastic.co/products/elasticsearch)
- [Apache Kafka](https://kafka.apache.org/)
- [SONG](https://github.com/overture-stack/SONG)

you can check the sample docker compose files under ./run/docker for containerized versions of elastic & kafka.
for SONG please check the SONG github repo [here](https://github.com/overture-stack/SONG/tree/develop/dev) 
on how to run it with docker. Or you can run it as jar.

## How to:
Note: if you don't/can't use the Makefile, look inside it for the shell commands and replicate them.
- Compile: `make` 
- Test: `make test`
- Package: `make package`
- Run:
    - Development:
        - `make docker-start-dev` starts the infrastructure containers
            - kafka
            - elastic search
            - other helper tools if you want like kafka rest proxy
        - `make run` OR start maestro (Maestro.java) from the IDE/cmd as a java application
    - Demo:
        - `make docker-start` starts meastro from a docker image along with all needed infrastructure


