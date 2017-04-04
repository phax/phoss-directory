# peppol-directory

[![Join the chat at https://gitter.im/phax/peppol-directory](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/phax/peppol-directory?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Current release (on Maven central): **0.4.0**

The official PEPPOL Directory (PD; former PEPPOL Yellow Pages - PYP) software. It is split into the following sub-projects (all require Java 8 except where noted):
  * `peppol-directory-businesscard` - the common Business Card API
  * `peppol-directory-api` - the common API for the indexer and the publisher incl. Lucene handling
  * `peppol-directory-indexer` - the PD indexer part
  * `peppol-directory-publisher` - the PD publisher web application
  * `peppol-directory-client` - a client library to be added to SMP servers to force indexing in the PD
  * `peppol-directory-client-jdk6` - a client library to be added to SMP servers to force indexing in the PD (Java 1.6)
  
Status as per 2017-04-44:
  * A test version is live on http://pyp.helger.com - search e.g. for "austria" 
  * The indexer part (incl. REST interface) is working properly. It just requires some fine tuning in the administration area.
  * The publisher website was started and the search already works. Check the live demo at http://pyp.helger.com for tests.
  * A Java library to be used in SMPs to communicate with the PD is available (`peppol-directory-client` for JDK 1.8 and `peppol-directory-client-jdk6` for JDK 1.6)
  * [phoss SMP Server](https://github.com/phax/peppol-smp-server) supports starting with version 4.1.2 the graphical editing of Business Card incl. the new `/businesscard` API.
  
Open tasks according to the design document:
  * The REST query API must be added to the publisher
  * The extended search for the UI must be added
  * An administration GUI (e.g. auditing of index actions) would be nice
  * The final appearance should be running on https 

# Building requirements
To build the PD software you need at least Java 1.8 and Apache Maven 3.x. Configuration for usage with Eclipse 4.6.x is contained in the repository.

Additionally to the contained projects you MAY need the latest SNAPSHOT of [ph-oton](https://github.com/phax/ph-oton) as part of your build environment. 

# PD Indexer
The PD Indexer is a REST component that is responsible for taking indexing requests from SMPs and processes them in a queue (PEPPOL SMP client certificate required). Only the PEPPOL participant identifiers are taken and the PD Indexer is responsible for querying the respective SMP data directly. Therefore the respective SMP must have the appropriate `Extension` element of the service group filled with the business information metadata as required by PD. Please see the PD specification draft on [Google Drive](https://drive.google.com/drive/folders/0B8Jct_iOJR9WfjJSS2dfdVdZYzBQMFotdmZoTXBZRl9Gd0cwdnB6cDZOQVlYbElrdEVVXzg)  for a detailed description of the required data format as well as for the REST interface.

# PD Publisher
The PD Publisher is the publicly accessible web site with listing and search functionality for certain participants.

---

My personal [Coding Styleguide](https://github.com/phax/meta/blob/master/CodeingStyleguide.md) |
On Twitter: <a href="https://twitter.com/philiphelger">@philiphelger</a>
