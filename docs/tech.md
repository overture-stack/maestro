
# Technical Design Goals
- Reactive
    - Event driven
    - Elastic
    - Resiliency & Fault tolerance
- Failure audit
       - Dead letters queue for faulty messages to be retried later and reviewed.
       - Human readable Error log
- Extendable: 
    separate domain from infrastructure & configuration

# Technologies & libraries
- Java 11 - OpenJDK 11
- Maven 3 (YOU NEED MAVEN 3.5+, if you don't want to use the wrapper or your IDE points to older version)
- lombok
- Spring boot 2
    - Spring webflux & project reactor
    - Spring retry
    - Spring Cloud
        - Streams (for Kafak integration)
        - config client
    - Spring boot admin client
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

# Code Structure 
The project is following the ports/adapters architecture, where the domain is completely isolated from external infrastructure and frameworks.

Two maven modules:

- Maestro domain: 
    - The core features and framework independent logic that is portable and contains the main indexing, rules, notifications logic as specified by the business features. Has packages like:
        - entities : contains POJOs and entities
        - api: the logic that fulfills the business features
        - ports: contains the interfaces needed by the api to communicate with anything outside the indexing context.

- Maestro app:
    - The main runnable (spring boot app)
    - Contains the infrastructure and adapters (ports implementations) that is needed to connect the domain with the outside world like elastic search, song web clients, configuration files etc. It also has the Spring framework configurations here to keep technologies outside of the domain.
