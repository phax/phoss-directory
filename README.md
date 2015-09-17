# peppol-yellow-pages
The official PEPPOL Yellow Pages (PYP) software. It is split into the following sub-projects:
  * `peppol-yellow-pages-api` - the common API for the indexer and the publisher incl. Lucene handling
  * `peppol-yellow-pages-indexer` - the PYP indexer part
  * `peppol-yellow-pages-publisher` - the PYP publisher web application
  
Status as per 2015-09-17: 
  * This project was started on 2015-08-31 so don't expect too much right now :)
  * The indexer part (incl. REST interface) looks quite good. Unit tests with concurrent indexing requests work flawlessly incl. Lucene based storage and lookup. The first real BusinessInformation lookup for `9915:test` also succeeded.
  * The next steps after finishing the Indexer are to work on the publisher web site. Except something available by approximately end of September 2015
  
# Building requirements
To build the PYP software you need at least Java 1.8 and Apache Maven 3.x. Configuration for usage with Eclipse 4.5 is contained in the repository.

Additionally to the contained projects you need the latest SNAPSHOT of [ph-oton](https://github.com/phax/ph-oton) as part of your build environment.

# PYP Indexer
The PYP indexer is a REST component that is responsible for taking indexing requests from SMPs and processes them in a queue (PEPPOL SMP client certificate required). Only the PEPPOL participant identifiers are taken and the PYP Indexer is responsible for querying the respective SMP data directly. Therefore the respective SMP must have the appropriate `Extension` element of the service group filled with the business information metadata as required by PYP. Please see the PYP specs on [Google Drive](https://drive.google.com/drive/folders/0B8Jct_iOJR9WfjJSS2dfdVdZYzBQMFotdmZoTXBZRl9Gd0cwdnB6cDZOQVlYbElrdEVVXzg)  for a detailed description of the required data format as well as for the REST interface.

# PYP Publisher
The PYP publisher is the publicly accessible web site with listing and search functionality for certain participants.

---

On Twitter: <a href="https://twitter.com/philiphelger">Follow @philiphelger</a>
