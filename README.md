# peppol-yellow-pages
The official PEPPOL Yellow Pages (PYP) software. It is split into the following sub-projects:
  * `peppol-yellow-pages-api` - the common API for the indexer and the publisher incl. Lucene handling
  * `peppol-yellow-pages-indexer` - the PYP indexer part
  * `peppol-yellow-pages-publisher` - the PYP publisher web application
  
This project was started on 2015-08-31 so don't expect too much right now :)

Status as per 2015-09-11: the indexer part (incl. REST interface) looks quite good. Unit tests with concurrent indexing requests work flawlessly incl. Lucene based storage and lookup. The next steps are to work on the publisher web site.
  
# Building requirements
To build the PYP software you need at least Java 1.8 and Apache Maven 3.x. Configuration for usage with Eclipse 4.5 is contained in the repository.

Additionally to the contained projects you need the latest SNAPSHOT of [ph-oton](https://github.com/phax/ph-oton) as part of your build environment.
Currently also the latest SNAPSHOT of [ph-commons](https://github.com/phax/ph-commons) is needed.

---

On Twitter: <a href="https://twitter.com/philiphelger">Follow @philiphelger</a>
