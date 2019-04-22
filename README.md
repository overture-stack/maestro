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
    - Resiliency & Fault tolerence
- Failure audit
- Runtime configurability
- Extendable: separate domain from infrastructure & configuration

## Technologies & libraries:
- Java 11 - OpenJDK 11
- Maven 3 (YOU NEED MAVEN 3.5+, if you don't want to use the wrapper or your IDE points to older version)
- lombok
- Spring boot 2
    - Spring webflux & project reactor
    - Spring data elasticsearch
    - Spring retry
    - Spring Cloud
        - Streams (for Kafak integration)
        - config client
    - Spring boot admin + client
- Elasticsearch 6+
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
The project is following the ports/adapters archeticture, where the domain is completely isolated from external infrastructure
and frameworks.
- Two maven modules:
    - maestro domain
      - the core features and framework independent logic that is portable and contains the main indexing, rules, notifications
      logic as specified by the business features. Has packages like:
          - entities : contains pojos and entities
          - api: the logic that fullfills the business features
          - ports: contains the interfaces needed by the api to communicate with anything outside the indexing context.

    - mastro app:
       - The main runnable (spring boot app)
       - Contains the infrastructure and adapters (ports implementations) that is needed to connect the domain
         with the outside world like elastic search, song web clients, configuration files etc.
         It also has the Spring framework configurations here to keep technologies outside of the domain.

## How to:
Note: if you don't/can't use the Makefile, look inside it for the shell commands and replicate them.
- Build: `make mvn-i`
- Test: `make mvn-t`
- Run:
    - Development:
        - `make doc-start-dev` starts the infrastructure containers
            - kafka
            - elastic search
            - other helper tools if you want like kafka rest proxy
        - `make boot-run`: start maestro (Maestro.java) from the IDE/cmd as a java application
    - Demo:
        - `make doc-start` starts meastro from a docker image along with all needed infrastructure


