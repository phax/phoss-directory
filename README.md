# phoss-directory

[![javadoc](https://javadoc.io/badge2/com.helger/phoss-directory-parent-pom/javadoc.svg)](https://javadoc.io/doc/com.helger/phoss-directory-parent-pom)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.helger/phoss-directory-parent-pom/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.helger/phoss-directory-parent-pom) 

The official Peppol Directory (PD; https://directory.peppol.eu) and TOOP Directory software (The Once-Only Project; www.toop.eu). It is split into the following sub-projects (all require Java 8 except where noted):
  * `phoss-directory-businesscard` - the common Business Card API
  * `phoss-directory-indexer` - the PD indexer part
  * `phoss-directory-publisher` - the PD publisher web application
  * `phoss-directory-client` - a client library to be added to SMP servers to force indexing in the PD
  * `phoss-directory-searchapi` - a client library for easier use of the Directory search REST API (since v0.7.2)
  
* Production version is available at https://directory.peppol.eu (for PEPPOL)
  * It can only handle participants registered at the SML
  * For the indexing REST API, a client certificate (SMP production) is needed 
* Test version is available at https://test-directory.peppol.eu
  * It can only handle participants registered at the SMK
  * For the indexing REST API, a client certificate (SMP test) is needed 
* A TOOP version is available at http://directory.acc.exchange.toop.eu/
  * It can only handle participants registered at the SMK at a specific DNS zone
  * For the indexing REST API, no client certificate is needed
  
* A Java library to be used in SMPs to communicate with the PD is available
* [phoss SMP Server](https://github.com/phax/phoss-smp) supports starting with version 4.1.2 the graphical editing of Business Card incl. the necessary `/businesscard` API.

# Building requirements

To build the PD software you need at least Java 1.8 and Apache Maven 3.x.

Additionally to the contained projects you *MAY* need the latest SNAPSHOT of [ph-oton](https://github.com/phax/ph-oton) as part of your build environment.

# PD Client

The PD client is a small Java library that uses Apache HttpClient to connect to an arbitrary phoss Directory Indexer to perform all the allowed operations (get, create/update, delete).

**Configuration resolution**

Note: this is new in v0.9.0.

The PD client uses the file `application.properties` for configuration.
The file `pd-client.properties` is also evaluated for backwards-compatibility reasons but with lower priority.

See https://github.com/phax/ph-commons#ph-config for the new resolution logic.

**Configuration file resolution (prior to 0.9.0)**

The client has its own configuration file that is resolved from one of the following locations (whatever is found first):
* A path denoted by the content of the Java system property `peppol.pd.client.properties.path`
* A path denoted by the content of the Java system property `pd.client.properties.path`
* An environment variable called `DIRECTORY_CLIENT_CONFIG` (since 0.8.4)
* A file with the filename `private-pd-client.properties` in the root of the classpath
* A file with the filename `pd-client.properties` in the root of the classpath

If no configuration file is found a warning is emitted and you cannot invoke any operations because the certificate configuration is missing.

**Configuration properties**

The following options are supported in the `pd-client.properties` file:
  * **keystore.type** (since v0.6.0) - the type of the keystore. Can be `JKS` or `PKCS12` (case insensitive). Defaults to `JKS`.
  * **keystore.path** - the path to the keystore where the SMP certificate is contained
  * **keystore.password** - the password to open the key store
  * **keystore.key.alias** - the alias in the key store that denotes the SMP key 
  * **keystore.key.password** - the password to open the key in the key store
  * **truststore.type** (since v0.6.0) - the type of the keystore. Can be `JKS` or `PKCS12` (case insensitive). Defaults to `JKS`.
  * **truststore.path** (since v0.5.1) - the path to the trust store, where the public certificates of the phoss Directory servers are contained. Defaults to `truststore/pd-client.truststore.jks`
  * **truststore.password** (since v0.5.1) - the password to open the truststore store. Defaults to `peppol`
  * **http.proxyHost** - the HTTP proxy host for `http` connections only. No default. 
  * **http.proxyPort** - the HTTP proxy port for `http` connections only. No default. 
  * **https.proxyHost** - the HTTP proxy host for `https` connections only. No default. 
  * **https.proxyPort** - the HTTP proxy port for `https` connections only. No default. 
  * **https.hostname-verification.disabled** (since v0.5.1) - a boolean value to indicate if https hostname verification should be disabled (`true`) or enabled (`false`). The default value is `true`. 
  * **proxy.username** (since v0.6.0) - the proxy username if http or https proxy is enabled. No default. 
  * **proxy.password** (since v0.6.0) - the proxy password if http or https proxy is enabled. No default.
  * **connect.timeout.ms** (since v0.6.0) - the connection timeout in milliseconds to connect to the server. The default value is `5000` (5 seconds). A value of `0` means indefinite. A value of `-1` means using the system default.
  * **request.timeout.ms** (since v0.6.0) - the request/read/socket timeout in milliseconds to read from the server. The default value is `10000` (10 seconds). A value of `0` means indefinite. A value of `-1` means using the system default.

Example PD client configuration file:

```ini
# Key store with SMP key (required)
keystore.type         = jks
keystore.path         = smp.pilot.jks
keystore.password     = password
keystore.key.alias    = smp.pilot
keystore.key.password = password

# Default trust store (optional)
truststore.type     = jks
truststore.path     = truststore/pd-client.truststore.jks
truststore.password = peppol

# TLS settings
https.hostname-verification.disabled = false
```

# PD Indexer

The PD Indexer is a REST component that is responsible for taking indexing requests from SMPs and processes them in a queue (PEPPOL SMP client certificate required). Only the PEPPOL participant identifiers are taken and the PD Indexer is responsible for querying the respective SMP data directly. Therefore the respective SMP must have the appropriate `Extension` element of the service group filled with the business information metadata as required by PD. Please see the [PD specification](https://github.com/OpenPEPPOL/documentation/blob/master/TransportInfrastructure/PEPPOL-EDN-Directory-1.1-2018-07-17.pdf) for a detailed description of the required data format as well as for the REST interface.


# PD Publisher

The PD Publisher is the publicly accessible web site with listing and search functionality for certain participants.

# News and noteworthy

* v0.9.9 - 2021-12-21
    * Updated to Log4J 2.17.0 because of CVE-2021-45105 - see https://logging.apache.org/log4j/2.x/security.html
* v0.9.8 - 2021-12-14
    * Updated to Log4J 2.16.0 because of CVE-2021-45046 - see https://www.lunasec.io/docs/blog/log4j-zero-day/
* v0.9.7 - 2021-12-10
    * Updated to Log4J 2.15.0 because of CVE-2021-44228 - see https://www.lunasec.io/docs/blog/log4j-zero-day/
* v0.9.6 - 2021-11-02
    * Improved support for JSON API in Business Card
* v0.9.5 - 2021-03-22
    * Updated to ph-commons 10
    * Updated to peppol-commons 8.4.0
    * Improved web UI customizability
* v0.9.4 - 2021-02-01
    * Fixed initialization order issue
* v0.9.3 - 2021-02-01
    * Updated to ph-commons 9.5.4
    * Updated to ph-dns 9.5.2
    * Updated to Jersey 2.32
    * Reduced lock contention
* v0.9.2 - 2020-09-24
    * Increased customizability
* v0.9.1 - 2020-09-18
    * Updated to Jakarta JAXB 2.3.3
* v0.9.0 - 2020-09-16
    * Updated to ph-commons 9.4.8
    * Changed the way how the configuration system works
* v0.8.8 - 2020-08-30
    * Updated to ph-commons 9.4.7
    * Updated to ph-oton 8.2.6
    * Updated to peppol-commons 8.1.7
    * Using Java 8 date and time classes for JAXB created classes
* v0.8.7 - 2020-05-27
    * Updated to ph-commons 9.4.4
    * Updated to new Maven groupIds
    * Improved logging
    * Improved resilience on identifier handling for stored entries
* v0.8.6 - 2020-02-19
    * URL decoding participant identifiers on indexation
    * Updated to ph-commons 9.4.0
* v0.8.5 - 2020-02-16
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
* v0.8.4 - 2020-01-24
    * Updated to Jersey 2.30
    * The Directory client has no more default truststore path and password
    * The Directory client configuration can now be read from the path denoted by the environment variable `DIRECTORY_CLIENT_CONFIG`
    * Updated the static texts changing `PEPPOL` to `Peppol`
* v0.8.3 - 2020-01-08
    * Added logo in the left top (using configuration property `webapp.applogo.image.path`)
    * Setting `Content-Length` HTTP header for the downloads
    * Made FavIcons customizable
    * Added rate limit for search API (using configuration property `rest.limit.requestspersecond`)
* v0.8.2 - 2019-10-14
    * Added support to download all Business Cards as CSV
    * Added support to download all Business Cards as XML but without the document types (see [issue #42](https://github.com/phax/phoss-directory/issues/42))
    * Class `PDBusinessCard` got a default JSON representation
    * Updated to Jersey 2.29.1
    * Added support to download all Participant IDs only as XML, JSON and CSV
* v0.8.1 - 2019-07-29
    * Updated to Jersey 2.29
    * `PDClientConfiguration` can now be re-initialized during runtime
    * Known document type identifiers and process identifiers can now be used (see [issue #13](https://github.com/phax/phoss-directory/issues/13))
    * Extended XML export to include the new document types (see [issue #41](https://github.com/phax/phoss-directory/issues/41))
    * Added page to see the current Index Queue
* v0.8.0 - 2019-06-27
    * Renamed project `peppol-directory` to `phoss-directory`
    * **Maven artifact IDs changed** from `peppol-directory*` to `phoss-directory*`
    * Updated to `peppol-commons` 7.0.0
    * Downgraded to Lucene 7.7.2
    * Fixed an issue with `total-result-count` and paging in the REST API (see [issue #39](https://github.com/phax/phoss-directory/issues/39))
    * Updated to Apache httpclient 4.5.9
    * Updated to ph-oton 8.2.0
    * Added a new internal page for importing identifiers
    * The internal format for exporting participant IDs was updated to be used in the import
* v0.7.2 - 2019-05-13
    * Added new submodule `peppol-directory-searchapi` with basic elements for using the query API and the response documents
    * Updated default truststore of `peppol-directory-client`
    * Updated to Lucene 8.1.0
* v0.7.1 - 2019-03-17
    * Added new method `PDBusinessCardHelper.parseBusinessCard`
    * Updated to Lucene 8.0.0
* v0.7.0 - 2018-12-02
    * Added a link on the UI to download all business cards as XML
    * Fixed the build timestamp property
    * Fixed error when showing ReIndex entries of non-existing participants when using `ESensUrlProvider`
    * Added the XML Schema for the API search results
    * Added the XML Schema for the export data
    * Added a page explaining the export data
    * Requires ph-commons 9.2.0
    * Updated UI to use Bootstrap 4.1
* v0.6.2 - 2018-10-17
    * If more hits are present than visible, it is displayed on the UI
    * Made the available SML information objects customizable
    * Removed the configuration item `sml.id` - either fixed SMP or all configured SMLs are queried upon indexing
    * Updated to Apache Lucene 7.5
    * Multilingual business entities are now supported via a new Business Card XML Schema - for Belgium
    * The query API response document layout for XML was changed. `name` has now multiplicity 1..n instead of 1..1.
    * The query API response document layout for JSON was changed. `name` is now an array instead of a `string`.
    * Multiple parallel queries on the PD are possible.  
* v0.6.1 - 2018-06-04
    * Avoid potential exception on invalid input parameters
    * Updated to Jersey 2.27
    * Updated to Apache Lucene 7.3
    * Improved handling of multiple search parameters in name, geoinfo and additionalInfo
    * Updated to peppol-commons 6.1.0
    * Updated to ph-commons 9.1.0
    * Introduced an internal "generic business card representation"
    * An initial "export all business cards" was created
* v0.6.0 - 2018-03-06
    * Updated to ph-commons 9.0.1
    * Updated to Apache Lucene 7.2.1
    * Fixed some issues (as #30)
    * Requires peppol-commons 6.0.1 for new OpenPEPPOL PKI v3
    * Added support for trusting an arbitrary number of client certificate issuers (for the server only)
    * Added support for configuring more than two truststores in pd.properties (for the server only)
    * Added support for usage in the TOOP4EU project
    * User interface texts can be changed from "PEPPOL Directory" to something else
    * The PD client configuration now includes connection and request timeout, as well as proxy credentials
* v0.5.1 - 2017-07-21
    * Extended `PDClient` to explicitly support a configurable truststore. A default truststore for the current setup is included.
    * PD client https hostname verification can now be 
    * PD client has now a custom exception callback to catch exceptions in the operations and handle them outside the client.
    * Removed the JDK 6 PD client because the ECC certificates used are only supported by JDK 7 onwards. The old version is anyway in the Maven central repository.
* v0.5.0 - 2017-07-12
    * Updated release for `https://directory.peppol.eu` and `https://test-directory.peppol.eu`

---

My personal [Coding Styleguide](https://github.com/phax/meta/blob/master/CodingStyleguide.md) |
On Twitter: <a href="https://twitter.com/philiphelger">@philiphelger</a> |
Kindly supported by [YourKit Java Profiler](https://www.yourkit.com)