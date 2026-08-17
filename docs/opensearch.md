# OpenSearch

Since v0.16.0 the Peppol Directory can store its search index in [AWS OpenSearch](https://aws.amazon.com/opensearch-service/)
instead of the local Apache Lucene index. The implementation lives in the Maven module
`phoss-directory-indexer-opensearch` and is based on the official
[opensearch-java](https://github.com/opensearch-project/opensearch-java) client.

The search index implementation is selected via the configuration property `searchindex.type`.
Each implementation registers itself via the SPI interface `com.helger.pd.indexer.searchindex.IPDIndexProviderSPI`:

| `searchindex.type` | Implementation | Provided by |
|---|---|---|
| `lucene` (default) | `com.helger.pd.indexer.lucene.PDLuceneIndex` | `phoss-directory-indexer-lucene` |
| `opensearch` | `com.helger.pd.indexer.opensearch.PDOpenSearchIndex` | `phoss-directory-indexer-opensearch` |

The module `phoss-directory-indexer` itself contains no search index implementation at all - exactly
one of the two modules above must be on the classpath of the web application.
`phoss-directory-publisher` depends on `phoss-directory-indexer-lucene`, so the default deployment
is unchanged. To run on OpenSearch instead, replace that dependency:

```xml
<dependency>
  <groupId>com.helger</groupId>
  <artifactId>phoss-directory-indexer-opensearch</artifactId>
  <version>x.y.z</version>
</dependency>
```

Both modules may be on the classpath at the same time - only the one selected by `searchindex.type`
is instantiated.

## Configuration properties

All properties go into the regular Peppol Directory configuration file (e.g. `private-pd.properties`).

| Property | Default | Description |
|---|---|---|
| `searchindex.type` | `lucene` | Set to `opensearch` to use OpenSearch |
| `opensearch.endpoint` | - | **Mandatory.** Full URL of the OpenSearch endpoint, e.g. `https://search-mydomain-xxxx.eu-west-1.es.amazonaws.com` or `http://localhost:9200` |
| `opensearch.index` | `peppol-directory` | Name of the OpenSearch index that holds the business entities |
| `opensearch.auth` | `aws` | `aws` for AWS Signature Version 4 signing, `none` for an unauthenticated endpoint |
| `opensearch.aws.region` | - | **Mandatory if `opensearch.auth=aws`.** The AWS region of the domain, e.g. `eu-west-1` |
| `opensearch.aws.service` | `es` | The AWS service name to sign for. `es` = managed OpenSearch Service domain, `aoss` = OpenSearch Serverless collection |
| `opensearch.index.autocreate` | `true` | Create the index including the Peppol Directory mapping if it does not exist yet |
| `opensearch.index.shards` | `1` | Number of primary shards - only used when the index is created by this application |
| `opensearch.index.replicas` | `1` | Number of replicas - only used when the index is created by this application |
| `opensearch.scroll.pagesize` | `1000` | Number of documents read per scroll request when *all* matching documents are requested |
| `opensearch.scroll.timeout.minutes` | `5` | Minutes a scroll context is kept alive on the server |

### Authentication

Only two authentication modes are supported:

* `aws` - AWS Signature Version 4 request signing using the **default AWS credentials provider chain**
  (environment variables, system properties, the `~/.aws/credentials` profile, container credentials,
  the EC2 instance profile, ...) - exactly like the existing S3 export (see [s3.md](s3.md)).
  The IAM principal needs the `es:ESHttpGet`, `es:ESHttpPost`, `es:ESHttpPut`, `es:ESHttpDelete` and
  `es:ESHttpHead` permissions on the domain.
* `none` - no authentication at all. Only suitable for a local test installation.

HTTP basic authentication (as used by the AWS "fine-grained access control" master user) is
deliberately **not** supported.

## Example configurations

Managed AWS OpenSearch Service domain:

```properties
searchindex.type = opensearch
opensearch.endpoint = https://search-peppol-directory-abcdef.eu-west-1.es.amazonaws.com
opensearch.aws.region = eu-west-1
```

AWS OpenSearch Serverless collection:

```properties
searchindex.type = opensearch
opensearch.endpoint = https://abcdef123456.eu-west-1.aoss.amazonaws.com
opensearch.aws.region = eu-west-1
opensearch.aws.service = aoss
```

Local OpenSearch in Docker (see below):

```properties
searchindex.type = opensearch
opensearch.endpoint = http://localhost:9200
opensearch.auth = none
opensearch.index.replicas = 0
```

## Local test setup with Docker

The security plugin is disabled, so that no TLS certificates and no credentials are needed:

```shell
docker run -d -p 9200:9200 -p 9600:9600 \
  -e "discovery.type=single-node" \
  -e "DISABLE_SECURITY_PLUGIN=true" \
  -e "OPENSEARCH_JAVA_OPTS=-Xms1g -Xmx1g" \
  --name pd-opensearch \
  opensearchproject/opensearch:2.19.3
```

Verify that it is up and running:

```shell
curl http://localhost:9200
```

Inspect the index that was created by the Peppol Directory:

```shell
curl http://localhost:9200/peppol-directory/_mapping?pretty
curl http://localhost:9200/peppol-directory/_count?pretty
```

Delete the index to start over:

```shell
curl -X DELETE http://localhost:9200/peppol-directory
```

## Conformance tests

The module `phoss-directory-indexer-conformance` contains the engine independent test suite that
every implementation of `IPDIndex` must pass:

| Class | Asserts |
|---|---|
| `AbstractPDIndexConformanceTest` | The `IPDIndex` contract - adding and reading back all field types, the max result count, `updateDocuments`, `deleteDocuments`, term/prefix/contains/boolean queries, `getSplitIntoTerms` and the native query cache |
| `AbstractPDStorageManagerConformanceTest` | `PDStorageManager` on top of an `IPDIndex` - creating, updating, reading and deleting business cards including the owner verification |

Each search index module derives from both classes:

* `PDLuceneIndexConformanceTest` / `PDLuceneStorageManagerConformanceTest` - always executed
* `PDOpenSearchIndexConformanceTest` / `PDOpenSearchStorageManagerConformanceTest` - executed if an
  OpenSearch is reachable at `http://localhost:9200`, otherwise the tests are skipped via
  `org.junit.Assume`. They use the separate index `peppol-directory-test` - never the production one
  - and delete all of its documents before every test method.

So simply starting the Docker container above and running `mvn test` executes the very same
assertions against both search engines. The GitHub Actions workflow starts the container as well,
so a divergence between the two engines fails the build.

Two things are deliberately **not** part of the conformance suite, because they are not part of the
`IPDIndex` contract:

* The order in which `searchAll` returns the matching documents. Apache Lucene returns the business
  entities of a participant in the order they were indexed, OpenSearch does not guarantee this
  across shards. Use a single shard if the entity order matters for the UI.
* The scoring of the results.

## Index mapping

The index is created with an explicit mapping and `"dynamic": false`, so unknown fields are stored
in the `_source` but never indexed. The mapping mirrors the Lucene field configuration one to one:

| Field | OpenSearch type | Lucene equivalent |
|---|---|---|
| `participantid` | `keyword` | `StringField` (not tokenized) |
| `doctypeid` | `keyword` | `StringField` (not tokenized) |
| `registrationdate` | `keyword` | `StringField` (not tokenized) |
| `name` | `text` (`standard`) | `TextField` (tokenized) |
| `ml-name` | `text` (`standard`) | `TextField` (tokenized) |
| `ml-language` | `keyword` | `StringField` (not tokenized) |
| `country` | `keyword` | `StringField` (not tokenized) |
| `geoinfo` | `text` (`standard`) | `TextField` (tokenized) |
| `identifiertype` | `text` (`standard`) | `TextField` (tokenized) |
| `identifier` | `text` (`standard`) | `TextField` (tokenized) |
| `website` | `text` (`standard`) | `TextField` (tokenized) |
| `bc-description` | `text` (`standard`) | `TextField` (tokenized) |
| `bc-name` | `text` (`standard`) | `TextField` (tokenized) |
| `bc-phone` | `text` (`standard`) | `TextField` (tokenized) |
| `bc-email` | `text` (`standard`) | `TextField` (tokenized) |
| `freetext` | `text` (`standard`) | `TextField` (tokenized) |
| `allfields` | `text` (`standard`), excluded from `_source` | `TextField` (tokenized, not stored) |
| `md-creationdt` | `long` (`index: false`) | `StoredField` (stored, not indexed) |
| `md-ownerid` | `keyword` | `StringField` (not tokenized) |
| `md-requestinghost` | `keyword` | `StringField` (not tokenized) |

Two details are relevant for query compatibility:

* The Lucene `StandardAnalyzer` (created without stop words) and the OpenSearch `standard` analyzer
  use the same tokenizer, the same lower casing and an empty stop word list. Therefore the term,
  prefix and wildcard queries of `PDQueryManager` return the same results in both engines.
* The `keyword` fields are mapped explicitly and therefore have **no** `ignore_above` limit. This is
  required because the Peppol document type identifiers are far longer than the 256 character limit
  that a dynamically mapped `keyword` field would get.

One Peppol business entity is one OpenSearch document. The document ID is generated by OpenSearch,
because all deletions happen by query and never by ID.

## Behavioural differences compared to Lucene

* **`updateDocuments` is not atomic.** Lucene can delete documents and add documents in a single
  atomic operation, OpenSearch cannot. `PDOpenSearchIndex` therefore first runs a `_delete_by_query`
  and afterwards a `_bulk` index request. A concurrent reader can see the participant with no
  business entity at all for a very short period of time.
* **All writes use `refresh=true`.** Otherwise the changes would only become visible after the
  index refresh interval (1 second by default), which would break the read-after-write expectations
  of `PDStorageManager` (e.g. counting the documents before deleting them). This makes every write
  slightly more expensive - the indexer processes one participant per call, so it is roughly one
  refresh per indexed participant.
* **Reading all documents uses the scroll API**, because a single search request is limited to
  `index.max_result_window` (10.000 by default) documents. AWS OpenSearch Serverless does not
  support the scroll API - a Serverless collection has therefore not been verified to work.
* **`_delete_by_query` uses `conflicts=proceed`**, so a concurrent update does not abort a deletion.
* **The result order is not guaranteed.** Apache Lucene indexes all business entities of a
  participant as one block and returns them in that order. OpenSearch scrolls sorted by `_doc`,
  which is the insertion order within a shard only.

## Migrating an existing Lucene index

There is no automatic migration. The recommended way to fill a fresh OpenSearch index is to
re-index all participants, either via the "Participant actions" page in the secure application area
or by importing a previously created business card export.
