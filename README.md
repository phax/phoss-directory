# phoss-directory

<!-- ph-badge-start -->
[![Sonatype Central](https://maven-badges.sml.io/sonatype-central/com.helger/phoss-directory-parent-pom/badge.svg)](https://maven-badges.sml.io/sonatype-central/com.helger/phoss-directory-parent-pom/)
[![javadoc](https://javadoc.io/badge2/com.helger/phoss-directory-indexer/javadoc.svg)](https://javadoc.io/doc/com.helger/phoss-directory-indexer)

> If this project saved you some time or made your day a little easier, a star would mean a lot — it helps others find it too.
<!-- ph-badge-end -->

The official Peppol Directory (PD; https://directory.peppol.eu).

This project is part of my Peppol solution stack. See https://github.com/phax/peppol for other components and libraries in that area.
 
This project is split into the following sub-projects:
* `phoss-directory-indexer` - the PD indexer part (requires Java 25 since v0.17.0)
* `phoss-directory-publisher` - the PD publisher web application (requires Java 25 since v0.17.0)
* `phoss-directory-client` - a client library to be added to SMP servers to force indexing in the PD (requires Java 17)
* `phoss-directory-searchapi` - a client library for easier use of the Directory search REST API (since v0.7.2; requires Java 17)

Previous modules:
* `phoss-directory-businesscard` - the common Business Card API - until v0.12.3; then moved to com.helger.peppol:peppol-directory-businesscard in https://github.com/phax/peppol-commons 
  
* Production version is available at https://directory.peppol.eu (for Peppol)
    * It can only handle participants registered at the SML
    * For the indexing REST API, a client certificate (SMP production) is needed 
* Test version is available at https://test-directory.peppol.eu
    * It can only handle participants registered at the SMK
    * For the indexing REST API, a client certificate (SMP test) is needed

# Building requirements

To build the PD software you need at least Java 25 and Apache Maven 3.x.

The two artifacts that are consumed by third parties - `phoss-directory-client` and `phoss-directory-searchapi` -
  are compiled for Java 17, so that SMP servers running on Java 17 can keep using them.
All other modules only ever run inside the Directory server itself and are compiled for Java 25.

Additionally to the contained projects you *MAY* need the latest SNAPSHOT of [ph-oton](https://github.com/phax/ph-oton) as part of your build environment.

# PD Client

The PD client is a small Java library that uses Apache HttpClient to connect to an arbitrary phoss Directory Indexer to perform all the allowed operations (get, create/update, delete).

## Client Configuration resolution

The PD client uses `ph-config` to resolve configuration items.
See https://github.com/phax/ph-commons/wiki/ph-config for the details on the resolution logic.

Note: the old file `pd-client.properties` is not evaluated anymore.

## Client Configuration properties

Note: the configuration properties were heavily renamed in v0.10.0. Previous old names are shown in brackets.

The following configuration items are supported by the PD Client:
* **`pdclient.keystore.type`** (old: **`keystore.type`**) (since v0.6.0) - the type of the keystore. Can be `JKS` or `PKCS12` (case insensitive). Defaults to `JKS`.
* **`pdclient.keystore.path`** (old: **`keystore.path`**) - the path to the keystore where the SMP certificate is contained
* **`pdclient.keystore.password`** (old: **`keystore.password`**) - the password to open the key store
* **`pdclient.keystore.key.alias`** (old: **`keystore.key.alias`**) - the alias in the key store that denotes the SMP key 
* **`pdclient.keystore.key.password`** (old: **`keystore.key.password`**) - the password to open the key in the key store
* **`pdclient.truststore.type`** (old: **`truststore.type`**) (since v0.6.0) - the type of the keystore. Can be `JKS` or `PKCS12` (case insensitive). Defaults to `JKS`.
* **`pdclient.truststore.path`** (old: **`truststore.path`**) (since v0.5.1) - the path to the trust store, where the public certificates of the phoss Directory servers are contained. Defaults to `truststore/pd-client.truststore.jks`
* **`pdclient.truststore.password`** (old: **`truststore.password`**) (since v0.5.1) - the password to open the truststore store. Defaults to `peppol`
* **`http.proxy.host`** (old: **`http.proxyHost`**) - the HTTP proxy host for HTTP connections only. No default.
* **`http.proxy.port`** (old: **`http.proxyPort`**) - the HTTP proxy port for `http` connections only. No default.
* Removed in 0.10.0: ~**`https.proxyHost`** - the HTTP proxy host for `https` connections only. No default.~
* Removed in 0.10.0: ~**`https.proxyPort`** - the HTTP proxy port for `https` connections only. No default.~
* **`http.proxy.username`** (old: **`proxy.username`**) (since v0.6.0) - the proxy username if http or https proxy is enabled. No default. 
* **`http.proxy.password`** (old: **`proxy.password`**) (since v0.6.0) - the proxy password if http or https proxy is enabled. No default.
* **`http.connect.timeout.ms`** (old: **`connect.timeout.ms`**) (since v0.6.0) - the connection timeout in milliseconds to connect to the server. The default value is `5000` (5 seconds). A value of `0` means indefinite. A value of `-1` means using the system default.
* **`http.response.timeout.ms`** (old: **`http.request.timeout.ms`** or **`request.timeout.ms`**) (since v0.10.3) - the response/request/read timeout in milliseconds to read from the server. The default value is `10000` (10 seconds). A value of `0` means indefinite. A value of `-1` means using the system default.
* **`https.hostname-verification.disabled`** (since v0.5.1) - a boolean value to indicate if https hostname verification should be disabled (`true`) or enabled (`false`). The default value is `true`.

Example PD Client configuration properties:

```ini
# Key store with SMP key (required)
pdclient.keystore.type         = pkcs12
pdclient.keystore.path         = smp-test.p12
pdclient.keystore.password     = password
pdclient.keystore.key.alias    = cert
pdclient.keystore.key.password = password

# Default trust store (optional)
pdclient.truststore.type     = pkcs12
# For Test:
pdclient.truststore.path     = truststore/2025/smp-test-truststore.p12
# For production:
# pdclient.truststore.path     = truststore/2025/smp-prod-truststore.p12
pdclient.truststore.password = peppol

# TLS settings
https.hostname-verification.disabled = false
```

# PD Indexer

The PD Indexer is a REST component that is responsible for taking indexing requests from SMPs and processes them in a queue 
(Peppol SMP client certificate required). 
Only the Peppol participant identifiers are taken and the PD Indexer is responsible for querying the respective SMP data directly. 
Therefore the respective SMP must have the appropriate `Extension` element of the service group filled with the business 
  information metadata as required by PD.
Please see the [PD specification](https://docs.peppol.eu/edelivery/directory/PEPPOL-EDN-Directory-1.1.1-2020-10-15.pdf) 
  for a detailed description of the required data format as well as for the REST interface.

## Indexer Parallelism Configuration

**Configuration properties:**

* **`indexer.maxparallel`** - The number of work items that are indexed in parallel, and therefore the number of SMP queries that are performed in parallel. Defaults to `4`.

Handling a single work item is dominated by waiting for the SMP to respond and not by CPU usage, and since v0.17.0 the
  indexing threads are virtual threads.
This value may therefore be raised way beyond the number of available cores.
Be aware that raising it directly increases the request rate that the Directory puts on the SMPs of the network.

## Indexer Shadowing Configuration

The PD Indexer supports shadowing of indexing requests to a downstream replicator service for migration purposes (e.g., PD2 migration). 
When enabled, successful indexing requests are replicated asynchronously as custom JSON events to a configured downstream URL.

**Configuration properties:**

* **`indexer.shadowing.enabled`** - Enable or disable indexer request shadowing. Defaults to `false`.
* **`indexer.shadowing.url`** - The downstream URL to send shadow events to. Required if shadowing is enabled.
* **`indexer.shadowing.timeout.ms`** - HTTP timeout in milliseconds for shadow requests. Defaults to `5000` (5 seconds).
* **`indexer.shadowing.interval.seconds`** - Interval in seconds for the dispatcher job to process queued events. Defaults to `60` seconds (1 minute).
* **`indexer.shadowing.secret`** - Optional secret string included in the `X-Shadow-Secret` HTTP header for authentication. If not set, no authentication header is sent.

**Shadow event format:**

Shadow events are sent as HTTP POST requests with JSON payload containing:
- `eventId` - Unique UUID for idempotency
- `createdAt` - ISO 8601 timestamp
- `operation` - Operation type (`CREATE_UPDATE` or `DELETE`)
- `participantId` - The participant identifier
- `requestingHost` - The requesting host
- `clientCertificate` - Object containing:
  - `sha256Fingerprint` - SHA-256 fingerprint of the client certificate (primary identity)
  - `subjectDN` - Certificate subject distinguished name
  - `issuerDN` - Certificate issuer distinguished name

**Operational notes:**

- Shadow events are persisted to disk (`shadow-events.xml`) before dispatch for crash safety
- A background job dispatches events at the configured interval (default: every 60 seconds)
- Failed events with non-retryable errors (HTTP 4xx) are moved to a dead-letter queue (`failed-shadow-events.xml`)
- Failed events with retryable errors (network issues, HTTP 5xx) remain in the queue for automatic retry
- Shadow failures never affect the original indexing request
- Assumes single application instance per data directory
- Optional authentication via `X-Shadow-Secret` header for securing the downstream endpoint

Example configuration:

```ini
# Indexer shadowing (disabled by default)
indexer.shadowing.enabled=false
indexer.shadowing.url=https://pd2-replicator.example.com/shadow-events
indexer.shadowing.timeout.ms=5000
indexer.shadowing.interval.seconds=60
indexer.shadowing.secret=your-secret-token-here
```


# PD Publisher

The PD Publisher is the publicly accessible web site with listing and search functionality for certain participants.

# News and noteworthy

v0.17.2 - work in progress
* Added the button "Re-index all entries now" to the "Dead Index List" page, to move all dead entries back into the indexing queue. See [#89](https://github.com/phax/phoss-directory/issues/89)
    * Each entry is queued with its original action type, so that the respective entry is also removed from the dead list
* Fixed that on startup the persisted indexer work items were read and executed before the Business Card provider was set, so that all of them failed with "No BusinessCard Provider is present." and were moved to the re-index list. See [#90](https://github.com/phax/phoss-directory/issues/90)
    * The `PDIndexerManager` constructor no longer starts any indexing activity - the new method `PDIndexerManager.startIndexing ()` schedules the re-index job and reads the persisted work items, and it must be called after the Business Card provider was set
    * `startIndexing ()` throws an `IllegalStateException` if no Business Card provider is present, so that a wrong startup order cannot happen unnoticed
* The export of all Business Cards and participants queries the search index only once per participant instead of once per participant and export format, reducing the number of index queries of a full export run by 75%. See [#88](https://github.com/phax/phoss-directory/issues/88)
    * All the export formats are now created in a single pass over the participants, so that each participant is read, parsed and converted to `PDStoredBusinessEntity` objects only once
    * **Backwards incompatible change**: the `ExportAllManager.writeFile*` methods were replaced by `ExportAllManager.exportAll (...)` that takes the export formats to be created
    * Added the interface `IExportAllHandler` and the base class `AbstractExportAllHandler` - one implementation per export format, all fed with the data of every participant
    * Fixed that an export format that failed to be created was nevertheless uploaded to S3, overwriting the previously good file with a truncated one. Now only successfully created files are uploaded
    * The export status shown in the administration UI now contains the export progress and the name of the export format that is currently uploaded

v0.17.1 - 2026-08-31
* Fixed a concurrency issue in the Apache Lucene search index that could return the Business Card of an unrelated participant, if the index was modified while a search was running (security advisory [GHSA-8qhv-6p5x-2437](https://github.com/phax/phoss-directory/security/advisories/GHSA-8qhv-6p5x-2437))
    * The internal Lucene document IDs of a search result are only valid for the index reader that created them, but they were resolved via an independently obtained index reader that may have been reopened in the meantime
    * `PDLucene` now uses a Lucene `SearcherManager`, so that all the documents of a search result are resolved with exactly the searcher that was used for searching, and so that an index reader that is in use is neither replaced nor closed underneath the caller
    * This also fixes that the previously used `DirectoryReader` was never closed when the index changed
    * **Backwards incompatible change**: `PDLucene.getDirectoryReader ()`, `PDLucene.getSearcher ()` and `PDLucene.getDocument (int)` were replaced by `PDLucene.acquireSearcher ()` and `PDLucene.releaseSearcher (IndexSearcher)` - the previous API could not be used in a thread-safe way
    * **Backwards incompatible change**: the interface `ILuceneDocumentProvider` was removed and the constructor of `AllDocumentsCollector` takes the document consumer only - the documents are now resolved from the leaf reader that is currently being searched
    * The `phoss-directory-indexer-opensearch` implementation was not affected
    * Added the test `PDLuceneIndexConcurrentSearchFuncTest` that searches in parallel to index modifications and verifies that every result belongs to the queried participant

v0.17.0 - 2026-08-30
* The modules that only ever run inside the Directory server itself are now compiled for Java 25: `phoss-directory-indexer`, `phoss-directory-indexer-lucene`, `phoss-directory-indexer-opensearch`, `phoss-directory-indexer-conformance` and `phoss-directory-publisher`
    * `phoss-directory-client` and `phoss-directory-searchapi` are still compiled for Java 17, so that SMP servers running on Java 17 can keep using them
    * Building the project therefore requires Java 25 - the new POM property `java.version.server` holds the version of the server side modules
* **Backwards incompatible change**: `IPDIndexQuery` is now a `sealed` interface that only permits the contained implementations, and `PDIndexQueryBool`, `PDIndexQueryContains`, `PDIndexQueryPrefix` and `PDIndexQueryTerm` are now `final`.
  The set of queries was documented as being closed since v0.16.0 - it is now enforced by the compiler
    * As a result `PDLuceneIndex` and `PDOpenSearchIndex` translate the queries with an exhaustive `switch` instead of a chain of `instanceof` checks, so that adding a new query type is a compile error in every implementation of `IPDIndex` instead of a runtime error in the one that was forgotten
    * The same applies to `EPDIndexQueryOccur` - adding a new occurrence is now a compile error in both implementations
* The indexer work items are now handled by virtual threads instead of a fixed pool of 4 platform threads
    * Added the new configuration property `indexer.maxparallel` to configure the number of work items that are handled in parallel. It defaults to `4`, so the load that the Directory puts on the SMPs of the network is unchanged unless the property is raised deliberately
    * **Backwards incompatible change**: the constructor of `IndexerWorkItemQueue` takes the number of parallel work items as the second parameter
    * Added `IndexerWorkItemQueue.getMaxParallel ()`
    * Added `PDServerConfiguration.getIndexerMaxParallel ()` and the constant `PDServerConfiguration.DEFAULT_INDEXER_MAX_PARALLEL`
    * Note: virtual threads are always daemon threads, whereas the previous indexer threads were not. `PDIndexerManager.close ()` stops the queue explicitly on shutdown, so the remaining work items are still persisted
    * The administration navbar shows the configured parallelism next to the index queue length
* `PDIndexExecutor` translates `EIndexerWorkItemType` with an exhaustive `switch` expression, so that adding a new work item type is a compile error
* Fixed that "Download results as XML" on the public search page ignored the maximum result count of the search (default 50, see the `max` query parameter) and exported all matching Business Cards instead
    * **Backwards incompatible change**: `ExportAllManager.queryAllContainedBusinessCardsAsXML` takes the maximum result count as the second parameter
    * **Backwards incompatible change**: `PDSessionSingleton.setLastQuery` takes the maximum result count as the second parameter
    * Added `PDSessionSingleton.getLastQueryMaxResultCount ()`
* Note: when running on Java 25, Apache Lucene 8.11.4 logs a warning that `sun.misc.Unsafe::invokeCleaner` is terminally deprecated, because that is how `MMapDirectory` unmaps index files.
  This is harmless today but the method will be removed in a future JRE - the OpenSearch based search index is not affected

v0.16.0 - 2026-08-19
* The publisher web UI was switched from Bootstrap 4 to Bootstrap 5, using `ph-oton-bootstrap5` instead of `ph-oton-bootstrap4`
    * No functional change - this is a pure UI framework update
* All search index access is now performed via the new search engine independent interface `IPDIndex` (package `com.helger.pd.indexer.searchindex`), so that a different search engine can be plugged in later
    * Added the search engine independent document model `PDIndexDocument` / `PDIndexField` replacing the usage of the Lucene `Document` and `Field` classes
    * Added the search engine independent query model `IPDIndexQuery` (package `com.helger.pd.indexer.searchindex.query`) replacing the usage of the Lucene `Query` and `Term` classes
    * Added `PDLuceneIndex` as the single implementation of `IPDIndex`, that continues to use Apache Lucene as before - all Lucene usage is now limited to the package `com.helger.pd.indexer.lucene`
    * Each `IPDIndexQuery` caches the search engine specific query created from it, so that executing the same query object twice (as the UI and the REST API do, to get the results and the total hit count) does not translate it twice - creating the Lucene `WildcardQuery` objects of a generic search costs roughly 0.5 milliseconds
    * The `PDQueryManager` methods `getXYZLuceneQuery` were renamed to `getXYZQuery` and return `IPDIndexQuery` instead of the Lucene `Query`
    * The `PDStringField` methods `getExactMatchTerm` and `getContainsTerm` were replaced by `getExactMatchQuery`, `getPrefixQuery` and `getContainsQuery`
    * `PDMetaManager.getLucene ()` was replaced by `PDMetaManager.getIndex ()`
    * `PDStorageManager.searchAtomic (Query, Collector)` was removed, because it was Lucene specific - use `searchAll` instead
    * No functional change - the index content and all search results stay the same
* The search index implementation is now pluggable and is selected via the new configuration property `searchindex.type` - see [docs/opensearch.md](docs/opensearch.md)
    * Each implementation registers itself via the new SPI interface `IPDIndexProviderSPI` and is resolved by the new class `PDIndexFactory`
    * **Backwards incompatible change**: the Apache Lucene implementation was moved from `phoss-directory-indexer` to the new submodule `phoss-directory-indexer-lucene` (`searchindex.type=lucene`, still the default). The module `phoss-directory-indexer` contains no search index implementation anymore, so exactly one implementation submodule must be added to the classpath. `phoss-directory-publisher` depends on `phoss-directory-indexer-lucene`, so the default deployment is unchanged
* Added the new submodule `phoss-directory-indexer-opensearch` containing `PDOpenSearchIndex`, an implementation of `IPDIndex` for AWS OpenSearch (`searchindex.type=opensearch`)
    * The OpenSearch endpoint is configured via the new properties starting with `opensearch.`
    * Supported authentication types are AWS Signature Version 4 (for managed AWS OpenSearch Service domains) and no authentication (for local testing) - HTTP basic authentication is not supported
    * Unlike Apache Lucene, OpenSearch cannot delete and add documents atomically, and it does not guarantee the order in which the business entities of a participant are returned
* Added the new submodule `phoss-directory-indexer-conformance` containing the search engine independent conformance test suite that every implementation of `IPDIndex` must pass
    * `AbstractPDIndexConformanceTest` asserts the `IPDIndex` contract, `AbstractPDStorageManagerConformanceTest` asserts `PDStorageManager` on top of it
    * The previous `PDStorageManagerTest` and `PDLuceneIndexTest` were folded into these classes, so the very same assertions now run against Apache Lucene and AWS OpenSearch
    * The OpenSearch conformance tests skip themselves if no OpenSearch is reachable at `http://localhost:9200`

v0.15.7 - 2026-08-05
* Updated to parent-pom 3.1.0, enabling Reproducible Builds
* Updated to ph-commons 12.3.3
    * Updated to BouncyCastle 1.85, fixing a myriad of CVEs
    * JSON writing now escapes all control characters (`U+0000`-`U+001F`) as `\u00XX` per RFC 8259 - relevant for the JSON export
    * External XML resource resolution no longer resolves remote URL schemes by default, to prevent Server Side Request Forgery (SSRF)
    * The internal soft maps used for caching are now thread-safe - previously concurrent reads could corrupt them and let cache eviction hang while holding the cache write lock
* Updated to ph-web 11.4.3
    * Incoming requests are now wrapped in a `SafeHttpServletRequest`, avoiding "The request object has been recycled" errors e.g. from the long running request monitor
    * The HTTP proxy configuration is now activated by the presence of `http.proxy.host` and `http.proxy.port` alone; `http.proxy.enabled` only acts as an explicit kill-switch when set to `false`
* Updated to ph-oton 10.3.0 and ph-oton-bootstrap4 10.2.0
    * Added throttling on login, if unknown user names are used
    * Updated to Jetty 12.1.10
* Updated to peppol-commons 12.6.1
    * Updated to Peppol eDEC Code Lists v9.7
    * The SMP client now verifies that the participant and document type identifiers contained in the SMP response match the requested ones, so SMPs that resolve identifiers case insensitively are detected during indexing
    * Removed the EC SML fallback in `EPeppolNetwork`
    * `peppol-smp-client` no longer requires a JAX-WS (Metro) runtime
* Updated to peppol-ui 0.9.19
* `PDClient` now logs the absolute URL of each invoked indexer request

v0.15.6 - 2026-05-18
* Improved the verbosity and high-load handling of the internal scheduler - hoping we're capturing the underlying issue

v0.15.5 - 2026-05-18
* Added indexer shadowing support to replicate successful indexing requests to a downstream service (e.g. for PD2 migration). See the "Indexer Shadowing Configuration" section above for details
* Added `Cache-Control: max-age=86400` header on all `/export/*` redirect responses to improve cacheability
* Added per-IP per-file rate limiting on export redirects (sliding 24h window, configurable via `export.limit.requestsperday`, default 3 requests)
* Enabled S3 multipart uploads in `S3Helper`
* Removed OSGI bundling from `phoss-directory-client`, `phoss-directory-indexer` and `phoss-directory-searchapi`

v0.15.4 - 2026-03-17
* REST API returns errors as valid `application/json` or `application/xml` and no longer as `text/plain`
* Added new configuration property `peppol.lookup.enabled`
* Added link to Peppol Lookup service if the search term is a participant ID (if enabled)
* Added link to lookup service also in the "Support" menu

v0.15.3 - 2026-02-13
* Showing the errors found during indexation on the UI for better support

v0.15.2 - 2026-01-23
* S3 buckets get a max-age of 6 hours to improve cachability
* The check for Peppol Participant Identifier Value syntax was improved to follow the rules from the Peppol Policy for use of Identifiers 4.4.0

v0.15.1 - 2026-01-19
* Requires at least Java 17 again - my bad
* Updated changelog
* Trying to get `Content-Disposition` to work

v0.15.0 - 2026-01-18
* Requires at least Java 21
* Instead of streaming the export files to local disk, they are now stream to S3
* Instead of reading the files from local disk, they are redirected to S3

v0.14.10 - 2025-12-30
* Updated to Peppol eDEC Code Lists v9.5
* Added new configuration propery `webapp.api.allow.origin` to configure `Access-Control-Allow-Origin` response header
* Removed support for the `pd.properties` and `private-pd.properties` configuration sources
* The indexation happens now in 4 parallel threads
* The participant details are now showing the Participant Identifier Scheme name instead of the agency providing it
* The search result list details (like Name and Country) are now consistently aligned between the different participants
* Added a small hint for invalid Belgian CBE numbers in the Participant Details
* Fixed an error in the `directory-export-v3.xsd`
* Implemented an initial version of the change log. See [#76](https://github.com/phax/phoss-directory/issues/76)
* Showing the current queue length in the title bar to more easily evaluate possible delays

v0.14.9 - 2025-11-16
* Updated to ph-commons 12.1.0
* Using JSpecify annotations

v0.14.8 - 2025-11-13
* Fixed an error, that document types were not correctly extracted, if the SMP response contains a percent encoded participant identifier (fixed in peppol-smp-client 12.1.2)
* Trying to disable DNSJava caches. See [#77](https://github.com/phax/phoss-directory/issues/77)

v0.14.7 - 2025-11-04
* Created an updated XML Schema v3 for the XML export
* Using stream based JSON and XML export to reduce memory usage during export

v0.14.6 - 2025-11-03
* Improved "Export all" handling so that the participant list is queried only once
* Also removed any locking on export, to avoid blocking access to the export data while exporting

v0.14.5 - 2025-11-03
* Fixed a potential `NullPointerException` if a participant identifier could not be parsed
* Internal ownership representation was changed to not use the serial number anymore, therefore deletion should also work after a certificate update

v0.14.4 - 2025-11-03
* Fixed resilience when loading stored values that are invalid identifiers - was blocking the export

v0.14.3 - 2025-11-02
* Updated to eDEC Code Lists v9.4
* In case of an HTTP 429 response, the `Retry-After` header is set to the seconds to wait
* Removed unwanted "Peppol " in front of some predefined document type names
* Improved internal error and progress handling for export all job
* Made sure the HTTP 429 response is properly documented on the REST API documentation page

v0.14.2 - 2025-10-07
* Fixed HTTP response charset of CSV exports

v0.14.1 - 2025-10-03
* The export job is now scheduled to happen on 2am - more deterministically
* Added support for Peppol G2 + G3 support in parallel
* Removed the contact page form, as it was not working anymore
* Removed public page login
* Updated to eDEC Code Lists v9.3
* Fixed links to peppol.org and updated spelling where necessary

v0.14.0 - 2025-08-27
* Requires Java 17 as the minimum version
* Updated to ph-commons 12.0.0
* Removed all deprecated methods marked for removal

v0.13.6 - 2025-05-14
* Updated dependencies
* The version of the exported files have changed, because the code list states were incorporated (XML: v2 -> v3; JSON: v1 -> v2)

v0.13.5 - 2024-08-22
* Added a CORS HTTP response header for the REST API. See [#68](https://github.com/phax/phoss-directory/issues/68)

v0.13.4 - 2024-07-30
* Updated to peppol-commons 9.5.0 with eDEC Code Lists v8.9

v0.13.3 - 2024-05-24
* Updated to peppol-commons 9.4.0

v0.13.2 - 2024-04-02
* Ensured Java 21 compatibility

v0.13.1 - 2024-03-22
* Fixed the `name` REST API query parameter

v0.13.0 - 2023-11-13
* Removed submodule `phoss-directory-businesscard` and using `peppol-directory-businesscard` from https://github.com/phax/peppol-commons instead
* Updated code lists to v8.7

v0.12.3 - 2023-10-27
* Fixed the name of the attribute for the client certificate retrieval (`jakarta.`)
* Added special handling for Peppol Wildcard identifiers on the UI

v0.12.2 - 2023-08-24
* Updated to ph-oton 9.2.0
* Updated code lists to v8.6

v0.12.1 - 2023-08-16
* Introducing class `PDResultListMarshaller` in favour of `PDSearchAPI(Reader|Validator|Writer)`
* Added a BusinessCard JSON export

v0.12.0 - 2023-02-25
* Using Java 11 as the baseline
* Using **Servlet API 5.0.0** as the baseline: **JakartaEE 9, Java 11+, Apache Tomcat v10.0.x, Jetty 11.x**
* Updated to Jersey 3.1.1
* Updated to ph-commons 11
* Updated the known names to eDEC Code List v8.3

v0.11.1 - 2025-10-01
* Add the Disclaimer on the website
* Update the known document type and process IDs to eDEC codelist v9.3
* Added rudimentary support for Wildcard identifiers
* Fixed a bug in the name search
* Removed the Twitter links
* Added a BusinessCard JSON export
* The export of data is constantly scheduled to 2am instead of startup time
* Added a CORS HTTP response header for the REST API. See [#68](https://github.com/phax/phoss-directory/issues/68)

v0.11.0 - 2022-12-19
* Updated to Lucene 8.x

v0.10.5 - 2022-11-25
* Improved logging of indexation
* SMP client configuration became more resilient

v0.10.4 - 2022-11-14
* Added new configuration parameter `smp.tls.trust-all` to disable the TLS certificate checks for the SMP client

v0.10.3 - 2022-08-17
* Updated to Apache Http Client v5.x
* Updated to ph-web 9.7.1
* Fixed an error in the REST API with the "name" parameter when multilingual names are used

v0.10.2 - 2022-03-28
* Removed the code for the handling of objects marked as deleted
* Improved the owner check upon deletion

v0.10.1 - 2022-03-09
* Added export of participant IDs with metadata

v0.10.0 - 2022-03-06
* Only the SP owning a Participant can delete it. That implies, that upon certificate change the simple deletion will not work. It is recommended to first index the participant, so that the new certificate is used, and than delete it with the new certificate.
* Added an Admin page to manually delete a participant without an owner check
* Showing metadata information on participant details, if the admin user is logged in
* Removed the "SMP implementations" page
* Added a possibility to hide or customize the "Contact us" page
* Changed the PD Client configuration properties, to start with `pdclient.` and align the HTTP properties with SMP client configuration
    * `keystore.type` is now `pdclient.keystore.type`
    * `keystore.path` is now `pdclient.keystore.path`
    * `keystore.password` is now `pdclient.keystore.password`
    * `keystore.key.alias` is now `pdclient.keystore.key.alias`
    * `keystore.key.password` is now `pdclient.keystore.key.password`
    * `truststore.type` is now `pdclient.truststore.type`
    * `truststore.path` is now `pdclient.truststore.path`
    * `truststore.password` is now `pdclient.truststore.password`
    * `http.proxyHost` is now `http.proxy.host`
    * `http.proxyPort` is now `http.proxy.port`
    * `proxy.username` is now `http.proxy.username`
    * `proxy.password` is now `http.proxy.password`
    * `connect.timeout.ms` is now `http.connect.timeout.ms`
    * `request.timeout.ms` is now `http.request.timeout.ms`
    * `https.proxyHost` is no longer supported
    * `https.proxyPort` is no longer supported
* Fixed the default search background image URL

v0.9.10 - 2022-02-24
* Prepare for internal cleanup to get rid of the legacy "deleted" flag

v0.9.9 - 2021-12-21
* Updated to Log4J 2.17.0 because of CVE-2021-45105 - see https://logging.apache.org/log4j/2.x/security.html

v0.9.8 - 2021-12-14
* Updated to Log4J 2.16.0 because of CVE-2021-45046 - see https://www.lunasec.io/docs/blog/log4j-zero-day/

v0.9.7 - 2021-12-10
* Updated to Log4J 2.15.0 because of CVE-2021-44228 - see https://www.lunasec.io/docs/blog/log4j-zero-day/

v0.9.6 - 2021-11-02
* Improved support for JSON API in Business Card

v0.9.5 - 2021-03-22
* Updated to ph-commons 10
* Updated to peppol-commons 8.4.0
* Improved web UI customizability

v0.9.4 - 2021-02-01
* Fixed initialization order issue

v0.9.3 - 2021-02-01
* Updated to ph-commons 9.5.4
* Updated to ph-dns 9.5.2
* Updated to Jersey 2.32
* Reduced lock contention

v0.9.2 - 2020-09-24
* Increased customizability

v0.9.1 - 2020-09-18
* Updated to Jakarta JAXB 2.3.3

v0.9.0 - 2020-09-16
* Updated to ph-commons 9.4.8
* Changed the way how the configuration system works

v0.8.8 - 2020-08-30
* Updated to ph-commons 9.4.7
* Updated to ph-oton 8.2.6
* Updated to peppol-commons 8.1.7
* Using Java 8 date and time classes for JAXB created classes

v0.8.7 - 2020-05-27
* Updated to ph-commons 9.4.4
* Updated to new Maven groupIds
* Improved logging
* Improved resilience on identifier handling for stored entries

v0.8.6 - 2020-02-19
* URL decoding participant identifiers on indexation
* Updated to ph-commons 9.4.0

v0.8.5 - 2020-02-16
* Finalized PEPPOL -> Peppol change
* Added registration date to the export data (see [issue #45](https://github.com/phax/phoss-directory/issues/45))
* Updated to peppol-commons 8.x
* Removed support for old PKI v2
* Made the identifier factory customizable to avoid duplicate entries
* Improved the internal Admin interface a bit
* Added possibility to automatically purge unwanted duplicate entries
* Updated the underlying UI libraries
* The lists of known document type IDs and process ID were updated
* Details about document types are now part of the export (see [issue #46](https://github.com/phax/phoss-directory/issues/46))
* Added the possibility to export search result as XML (see [issue #43](https://github.com/phax/phoss-directory/issues/43))
* Enforcing the `PDClient` proxy configuration to be part of `PDHttpClientSettings`
* Improved internal error resilience
* Fixed a validation that broken the daily export because of invalid PD data
* Updated to ph-web 9.1.9
* Changed the internal `PDClient` HTTP configuration API to use `HttpClientSettings` (backwards incompatible change) 
* The `PDClient` now checks for the key alias in a case insensitive manner (improved resilience) 

v0.8.4 - 2020-01-24
* Updated to Jersey 2.30
* The Directory client has no more default truststore path and password
* The Directory client configuration can now be read from the path denoted by the environment variable `DIRECTORY_CLIENT_CONFIG`
* Updated the static texts changing `PEPPOL` to `Peppol`

v0.8.3 - 2020-01-08
* Added logo in the left top (using configuration property `webapp.applogo.image.path`)
* Setting `Content-Length` HTTP header for the downloads
* Made FavIcons customizable
* Added rate limit for search API (using configuration property `rest.limit.requestspersecond`)

v0.8.2 - 2019-10-14
* Added support to download all Business Cards as CSV
* Added support to download all Business Cards as XML but without the document types (see [issue #42](https://github.com/phax/phoss-directory/issues/42))
* Class `PDBusinessCard` got a default JSON representation
* Updated to Jersey 2.29.1
* Added support to download all Participant IDs only as XML, JSON and CSV

v0.8.1 - 2019-07-29
* Updated to Jersey 2.29
* `PDClientConfiguration` can now be re-initialized during runtime
* Known document type identifiers and process identifiers can now be used (see [issue #13](https://github.com/phax/phoss-directory/issues/13))
* Extended XML export to include the new document types (see [issue #41](https://github.com/phax/phoss-directory/issues/41))
* Added page to see the current Index Queue

v0.8.0 - 2019-06-27
* Renamed project `peppol-directory` to `phoss-directory`
* **Maven artifact IDs changed** from `peppol-directory*` to `phoss-directory*`
* Updated to `peppol-commons` 7.0.0
* Downgraded to Lucene 7.7.2
* Fixed an issue with `total-result-count` and paging in the REST API (see [issue #39](https://github.com/phax/phoss-directory/issues/39))
* Updated to Apache httpclient 4.5.9
* Updated to ph-oton 8.2.0
* Added a new internal page for importing identifiers
* The internal format for exporting participant IDs was updated to be used in the import

v0.7.2 - 2019-05-13
* Added new submodule `peppol-directory-searchapi` with basic elements for using the query API and the response documents
* Updated default truststore of `peppol-directory-client`
* Updated to Lucene 8.1.0

v0.7.1 - 2019-03-17
* Added new method `PDBusinessCardHelper.parseBusinessCard`
* Updated to Lucene 8.0.0

v0.7.0 - 2018-12-02
* Added a link on the UI to download all business cards as XML
* Fixed the build timestamp property
* Fixed error when showing ReIndex entries of non-existing participants when using `ESensUrlProvider`
* Added the XML Schema for the API search results
* Added the XML Schema for the export data
* Added a page explaining the export data
* Requires ph-commons 9.2.0
* Updated UI to use Bootstrap 4.1

v0.6.2 - 2018-10-17
* If more hits are present than visible, it is displayed on the UI
* Made the available SML information objects customizable
* Removed the configuration item `sml.id` - either fixed SMP or all configured SMLs are queried upon indexing
* Updated to Apache Lucene 7.5
* Multilingual business entities are now supported via a new Business Card XML Schema - for Belgium
* The query API response document layout for XML was changed. `name` has now multiplicity 1..n instead of 1..1.
* The query API response document layout for JSON was changed. `name` is now an array instead of a `string`.
* Multiple parallel queries on the PD are possible.  

v0.6.1 - 2018-06-04
* Avoid potential exception on invalid input parameters
* Updated to Jersey 2.27
* Updated to Apache Lucene 7.3
* Improved handling of multiple search parameters in name, geoinfo and additionalInfo
* Updated to peppol-commons 6.1.0
* Updated to ph-commons 9.1.0
* Introduced an internal "generic business card representation"
* An initial "export all business cards" was created

v0.6.0 - 2018-03-06
* Updated to ph-commons 9.0.1
* Updated to Apache Lucene 7.2.1
* Fixed some issues (as #30)
* Requires peppol-commons 6.0.1 for new OpenPEPPOL PKI v3
* Added support for trusting an arbitrary number of client certificate issuers (for the server only)
* Added support for configuring more than two truststores in pd.properties (for the server only)
* Added support for usage in the TOOP4EU project
* User interface texts can be changed from "PEPPOL Directory" to something else
* The PD client configuration now includes connection and request timeout, as well as proxy credentials

v0.5.1 - 2017-07-21
* Extended `PDClient` to explicitly support a configurable truststore. A default truststore for the current setup is included.
* PD client https hostname verification can now be 
* PD client has now a custom exception callback to catch exceptions in the operations and handle them outside the client.
* Removed the JDK 6 PD client because the ECC certificates used are only supported by JDK 7 onwards. The old version is anyway in the Maven central repository.

v0.5.0 - 2017-07-12
* Updated release for `https://directory.peppol.eu` and `https://test-directory.peppol.eu`

---

My personal [Coding Styleguide](https://github.com/phax/meta/blob/master/CodingStyleguide.md) |
It is appreciated if you star the GitHub project if you like it.
