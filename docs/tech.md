# Technologies & Libraries

- Node.js v16.14 or greater
- PNPM package manager
- Elasticsearch 7+
- Apache Kafka
- Testing libraries:
  - Mocha
  - Chai
  - Testcontainers

# Code Structure

The project is organized as a monorepo workspace, where each application and package is located in its own dedicated folder:

```
apps/
├─ server/
packages/
├─ common/
├─ indexer-client/
├─ maestro-provider/
├─ repository/

```

- **Maestro Server**: The main runnable Express Server. Exposes a set of HTTP API routes that serve as the interface between the Maestro provider and external systems.

- Maestro Common: Designed to centralize common utilities, reusable functions, and TypeScript type definitions.

- Maestro Indexer Client: Abstracts communication with Elasticsearch clients, supporting both version 7 and version 8.

- Maestro Provider: The core features and provider independent logic that is portable and contains the main indexing, rules, notifications logic as specified by the business features.

- Maestro Repository: Designed to manage interactions with data source repositories. It serves as the central interface for retrieving data from various repositories, such as **Song** and **Lyric**, ensuring a streamlined and consistent approach to data access.
