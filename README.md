# phoss-directory

[![javadoc](https://javadoc.io/badge2/com.helger/phoss-directory-indexer/javadoc.svg)](https://javadoc.io/doc/com.helger/phoss-directory-indexer)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.helger/phoss-directory-parent-pom/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.helger/phoss-directory-parent-pom) 

The official Peppol Directory (PD; https://directory.peppol.eu).

This project is part of my Peppol solution stack. See https://github.com/phax/peppol for other components and libraries in that area.
 
This project is split into the following sub-projects (all require Java 11 or newer):
* `phoss-directory-indexer` - the PD indexer part
* `phoss-directory-publisher` - the PD publisher web application
* `phoss-directory-client` - a client library to be added to SMP servers to force indexing in the PD
* `phoss-directory-searchapi` - a client library for easier use of the Directory search REST API (since v0.7.2)

Previous modules:
* `phoss-directory-businesscard` - the common Business Card API - until v0.12.3; then moved to com.helger.peppol:peppol-directory-businesscard in https://github.com/phax/peppol-commons 
  
* Production version is available at https://directory.peppol.eu (for Peppol)
    * It can only handle participants registered at the SML
    * For the indexing REST API, a client certificate (SMP production) is needed 
* Test version is available at https://test-directory.peppol.eu
    * It can only handle participants registered at the SMK
    * For the indexing REST API, a client certificate (SMP test) is needed

# Building requirements

To build the PD software you need at least Java 11 and Apache Maven 3.x.

Additionally to the contained projects you *MAY* need the latest SNAPSHOT of [ph-oton](https://github.com/phax/ph-oton) as part of your build environment.

# PD Client

The PD client is a small Java library that uses Apache HttpClient to connect to an arbitrary phoss Directory Indexer to perform all the allowed operations (get, create/update, delete).

## Client Configuration resolution

The PD client uses `ph-config` to resolve configuration items.
See https://github.com/phax/ph-commons/wiki/ph-config for the details on the resolution logic.

Note: the old file `pd-client.properties` is still evaluated for backwards-compatibility reasons but with lower priority. It will be removed in the future.

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


# PD Publisher

The PD Publisher is the publicly accessible web site with listing and search functionality for certain participants.

# News and noteworthy

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
