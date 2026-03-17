# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**phoss-directory** is the official Peppol Directory (PD) — a secure, searchable registry for Peppol e-delivery network participants. Production instance: https://directory.peppol.eu

## Build Commands

Requires Java 17+ and Apache Maven 3.x. May also need the latest SNAPSHOT of [ph-oton](https://github.com/phax/ph-oton).

```bash
# Build entire project
mvn clean install

# Run all tests
mvn test

# Run a single test class
mvn test -pl phoss-directory-indexer -Dtest=PDLuceneTest

# Run a single test method
mvn test -pl phoss-directory-client -Dtest=PDClientTest#testTestServer
```

## Module Architecture

Four Maven modules under the parent POM (`com.helger:phoss-directory-parent-pom`):

- **phoss-directory-indexer** — REST service that receives indexing requests from SMPs (requires Peppol SMP client certificate). Queries SMP data directly and stores it in a **Lucene 8.x** index. Core classes: `PDLucene`, `PDStorageManager`, `PDStoredBusinessEntity`.

- **phoss-directory-publisher** — WAR web application providing search UI and REST API. Built on **ph-oton** (web framework) with Bootstrap 4. Handles bulk exports (XML/JSON/CSV) streamed to **AWS S3**. Deployed to Tomcat 10.x or Jetty 11.x (Jakarta EE 9 / Servlet 5.0).

- **phoss-directory-client** — Java client library for SMP servers to push indexing requests to the PD indexer. Uses Apache HttpClient with client certificate auth. Configured via `ph-config` resolution.

- **phoss-directory-searchapi** — JAXB-based library defining the search REST API data structures. XSD schemas in `src/main/resources/schemas/` for directory export (v1-v3) and search results.

## Key Frameworks & Libraries

- **ph-commons / ph-oton** — Helger's commons and web framework (core infrastructure)
- **peppol-commons** — Peppol protocol specifications and identifier handling
- **Apache Lucene 8.11.x** — Full-text search indexing (indexer module)
- **Jersey 3.1.x** — JAX-RS REST implementation
- **JAXB** — XML/JSON marshalling with code generation from XSD schemas
- **AWS SDK v2** — S3 integration for export files
- **JSpecify** — Null-safety annotations

## Code Generation

- JAXB classes are generated from XSD schemas via `jaxb-maven-plugin` into `generated/` directories
- JS/CSS minification via `ph-jscompress-maven-plugin` and `ph-csscompress-maven-plugin` in the publisher module

## Package Structure

All modules use the `com.helger.pd` base package:
- `com.helger.pd.indexer.*` — indexer (clientcert, lucene, storage, rest, mgr, reindex, job)
- `com.helger.pd.publisher.*` — publisher (servlet, ui, app, search, exportall, aws)
- `com.helger.pd.client.*` — client (PDClient, PDClientConfiguration)
- `com.helger.pd.searchapi.*` — search API data types

## CI/CD

GitHub Actions workflow (`.github/workflows/maven.yml`) tests on Java 17, 21, and 25. LocalStack is used for S3 integration testing.
