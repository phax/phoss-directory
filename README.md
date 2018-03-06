# peppol-directory

[![Join the chat at https://gitter.im/phax/peppol-directory](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/phax/peppol-directory?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Current release (on Maven central): **0.6.0**

The official PEPPOL Directory (PD; former PEPPOL Yellow Pages - PYP) software. It is split into the following sub-projects (all require Java 8 except where noted):
  * `peppol-directory-businesscard` - the common Business Card API
  * `peppol-directory-api` - the common API for the indexer and the publisher incl. Lucene handling
  * `peppol-directory-indexer` - the PD indexer part
  * `peppol-directory-publisher` - the PD publisher web application
  * `peppol-directory-client` - a client library to be added to SMP servers to force indexing in the PD
  
Deprecated sub-projects:  
  * `peppol-directory-client-jdk6` - a client library to be added to SMP servers to force indexing in the PD (Java 1.6) - only available until v0.5.0. Because of ECC certificate usage only available from Java 7 it doesn't make sense to work on this any longer.
  
Status as per 2018-03-06:
  * Production version is available at https://directory.peppol.eu
    * It can only handle participants registered at the SML
    * For the indexing REST API, a client certificate (SMP production) is needed 
  * Test version is available at https://test-directory.peppol.eu
    * It can only handle participants registered at the SMK
    * For the indexing REST API, a client certificate (SMP test) is needed 
  * A Java library to be used in SMPs to communicate with the PD is available
  * [phoss SMP Server](https://github.com/phax/peppol-smp-server) supports starting with version 4.1.2 the graphical editing of Business Card incl. the new `/businesscard` API.
  
Open tasks according to the design document:
  * The REST query API must be added to the publisher
  * The extended search for the UI must be added
  
# News and noteworthy

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
  * Updated release for https://directory.peppol.eu and https://test-directory.peppol.eu

# Building requirements
To build the PD software you need at least Java 1.8 and Apache Maven 3.x. Configuration for usage with Eclipse Neon and Oxygen is contained in the repository.

Additionally to the contained projects you *MAY* need the latest SNAPSHOT of [ph-oton](https://github.com/phax/ph-oton) as part of your build environment.

# PD Client

The PD client is a small Java library that uses Apache HttpClient to connect to an arbitrary PEPPOL Directory Indexer to perform all the allowed operations (get, create/update, delete).
The client has its own configuration file that is resolved from one of the following locations (whatever is found first):
* A path denoted by the content of the Java system property `peppol.pd.client.properties.path`
* A path denoted by the content of the Java system property `pd.client.properties.path`
* A file with the filename `private-pd-client.properties` in the root of the classpath
* A file with the filename `pd-client.properties` in the root of the classpath

If no configuration file is found a warning is emitted and you cannot invoke any operations because the certificate configuration is missing.

The following options are supported in the `pd-client.properties` file:
  * **keystore.type** (since v0.6.0) - the type of the keystore. Can be `JKS` or `PKCS12` (case insensitive). Defaults to `JKS`.
  * **keystore.path** - the path to the keystore where the SMP certificate is contained
  * **keystore.password** - the password to open the key store
  * **keystore.key.alias** - the alias in the key store that denotes the SMP key 
  * **keystore.key.password** - the password to open the key in the key store
  * **truststore.type** (since v0.6.0) - the type of the keystore. Can be `JKS` or `PKCS12` (case insensitive). Defaults to `JKS`.
  * **truststore.path** (since v0.5.1) - the path to the trust store, where the public certificates of the PEPPOL Directory servers are contained. Defaults to `truststore/pd-client.truststore.jks`
  * **truststore.password** (since v0.5.1) - the password to open the truststore store. Defaults to `peppol`
  * **http.proxyHost** - the HTTP proxy host for `http` connections only. No default. 
  * **http.proxyPort** - the HTTP proxy port for `http` connections only. No default. 
  * **https.proxyHost** - the HTTP proxy host for `https` connections only. No default. 
  * **https.proxyPort** - the HTTP proxy port for `https` connections only. No default. 
  * **https.hostname-verification.disabled** (since v0.5.1) - a boolean value to indicate if https hostname verification should be disabled (`true`) or enabled (`false`). The current setup of the PEPPOL Directory servers require you to use `true` here. The default value is `true`. 
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
# Must be disabled for https://test-directory.peppol.eu and https://directory.peppol.eu
https.hostname-verification.disabled = true
```

# PD Indexer
The PD Indexer is a REST component that is responsible for taking indexing requests from SMPs and processes them in a queue (PEPPOL SMP client certificate required). Only the PEPPOL participant identifiers are taken and the PD Indexer is responsible for querying the respective SMP data directly. Therefore the respective SMP must have the appropriate `Extension` element of the service group filled with the business information metadata as required by PD. Please see the PD specification draft on [Google Drive](https://drive.google.com/drive/folders/0B8Jct_iOJR9WfjJSS2dfdVdZYzBQMFotdmZoTXBZRl9Gd0cwdnB6cDZOQVlYbElrdEVVXzg)  for a detailed description of the required data format as well as for the REST interface.

# PD Publisher
The PD Publisher is the publicly accessible web site with listing and search functionality for certain participants.

---

My personal [Coding Styleguide](https://github.com/phax/meta/blob/master/CodingStyleguide.md) |
On Twitter: <a href="https://twitter.com/philiphelger">@philiphelger</a>
