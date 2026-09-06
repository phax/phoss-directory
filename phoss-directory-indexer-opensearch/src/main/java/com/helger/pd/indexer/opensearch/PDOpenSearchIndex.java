/*
 * Copyright (C) 2026 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.pd.indexer.opensearch;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.apache.hc.core5.http.HttpHost;
import org.jspecify.annotations.NonNull;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.Conflicts;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.Refresh;
import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.Time;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.CountResponse;
import org.opensearch.client.opensearch.core.DeleteByQueryResponse;
import org.opensearch.client.opensearch.core.InfoResponse;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.search.TotalHits;
import org.opensearch.client.opensearch.indices.AnalyzeResponse;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.opensearch.client.opensearch.indices.analyze.AnalyzeToken;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.aws.AwsSdk2Transport;
import org.opensearch.client.transport.aws.AwsSdk2TransportOptions;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.CheckForSigned;
import com.helger.annotation.Nonempty;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsLinkedHashMap;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsOrderedMap;
import com.helger.pd.indexer.searchindex.EPDIndexFieldTokenize;
import com.helger.pd.indexer.searchindex.IPDIndex;
import com.helger.pd.indexer.searchindex.PDIndexDocument;
import com.helger.pd.indexer.searchindex.PDIndexField;
import com.helger.pd.indexer.searchindex.query.IPDIndexQuery;
import com.helger.pd.indexer.searchindex.query.PDIndexQueryBool;
import com.helger.pd.indexer.searchindex.query.PDIndexQueryContains;
import com.helger.pd.indexer.searchindex.query.PDIndexQueryMatchAll;
import com.helger.pd.indexer.searchindex.query.PDIndexQueryPrefix;
import com.helger.pd.indexer.searchindex.query.PDIndexQueryTerm;
import com.helger.pd.indexer.storage.CPDStorage;
import com.helger.pd.indexer.storage.field.PDField;

import jakarta.annotation.Nullable;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

/**
 * The AWS OpenSearch based implementation of {@link IPDIndex}. It translates the search engine
 * independent documents and queries into their OpenSearch counterparts.<br>
 * All string fields that are tokenized are mapped to the OpenSearch type <code>text</code> using
 * the <code>standard</code> analyzer, all other string fields are mapped to the type
 * <code>keyword</code>. That way the term, prefix and wildcard queries behave exactly like they do
 * with Apache Lucene, because the Lucene <code>StandardAnalyzer</code> and the OpenSearch
 * <code>standard</code> analyzer use the same tokenizer, the same lower casing and an empty stop
 * word list.<br>
 * <b>Note:</b> unlike Apache Lucene, OpenSearch cannot delete documents and add documents in a
 * single atomic operation. {@link #updateDocuments(PDIndexQueryTerm, List)} therefore first deletes
 * and afterwards adds the new documents, meaning that a concurrent reader may see the participant
 * with no business entity at all for a very short period of time.
 *
 * @author Philip Helger
 * @since 0.16.0
 */
public class PDOpenSearchIndex implements IPDIndex
{
  /** The name of the OpenSearch analyzer that resembles the Lucene StandardAnalyzer */
  public static final String ANALYZER_STANDARD = "standard";

  private static final Logger LOGGER = LoggerFactory.getLogger (PDOpenSearchIndex.class);

  private final OpenSearchTransport m_aTransport;
  private final OpenSearchClient m_aClient;
  private final String m_sIndexName;
  private final String m_sEndpointURL;
  private final AtomicBoolean m_aClosing = new AtomicBoolean (false);

  /**
   * Default constructor using the configuration properties starting with <code>opensearch.</code>.
   *
   * @throws IOException
   *         On IO error
   */
  public PDOpenSearchIndex () throws IOException
  {
    this (createTransportFromConfiguration (),
          PDOpenSearchConfiguration.getIndexName (),
          PDOpenSearchConfiguration.getEndpointURL ());
  }

  /**
   * Constructor with an existing transport.
   *
   * @param aTransport
   *        The OpenSearch transport to be used. May not be <code>null</code>. It is closed together
   *        with this index.
   * @param sIndexName
   *        The name of the OpenSearch index to work on. May neither be <code>null</code> nor empty.
   * @param sEndpointURL
   *        The endpoint URL - for display purposes only. May be <code>null</code>.
   * @throws IOException
   *         On IO error
   */
  public PDOpenSearchIndex (@NonNull final OpenSearchTransport aTransport,
                            @NonNull @Nonempty final String sIndexName,
                            @Nullable final String sEndpointURL) throws IOException
  {
    ValueEnforcer.notNull (aTransport, "Transport");
    ValueEnforcer.notEmpty (sIndexName, "IndexName");

    m_aTransport = aTransport;
    m_aClient = new OpenSearchClient (aTransport);
    m_sIndexName = sIndexName;
    m_sEndpointURL = sEndpointURL;

    try
    {
      if (PDOpenSearchConfiguration.isIndexAutoCreate ())
        _createIndexIfNotExisting ();
    }
    catch (final IOException | RuntimeException ex)
    {
      // Don't leak the transport if the index cannot be prepared
      aTransport.close ();
      throw ex;
    }

    LOGGER.info ("OpenSearch index operating on '" + sEndpointURL + "' index '" + sIndexName + "'");
  }

  /**
   * Create the OpenSearch transport based on the configuration properties.
   *
   * @return The created transport. Never <code>null</code>.
   * @throws IllegalStateException
   *         If the configuration is incomplete
   */
  @NonNull
  public static OpenSearchTransport createTransportFromConfiguration ()
  {
    final String sEndpointURL = PDOpenSearchConfiguration.getEndpointURL ();
    if (StringHelper.isEmpty (sEndpointURL))
      throw new IllegalStateException ("The configuration property opensearch.endpoint is required");

    final EPDOpenSearchAuthType eAuthType = PDOpenSearchConfiguration.getAuthType ();
    switch (eAuthType)
    {
      case AWS_SIGV4:
      {
        final String sRegion = PDOpenSearchConfiguration.getAWSRegion ();
        if (StringHelper.isEmpty (sRegion))
          throw new IllegalStateException ("The configuration property opensearch.aws.region is required if opensearch.auth is '" +
                                           eAuthType.getID () +
                                           "'");

        // The AwsSdk2Transport expects the host name only - without scheme and without port
        final URI aURI = URI.create (sEndpointURL);
        final String sHost = aURI.getHost ();
        if (StringHelper.isEmpty (sHost))
          throw new IllegalStateException ("Failed to determine the host name of the OpenSearch endpoint '" +
                                           sEndpointURL +
                                           "'");

        LOGGER.info ("Configuring OpenSearch for AWS SigV4 mode on host '" +
                     sHost +
                     "' in region '" +
                     sRegion +
                     "' for service '" +
                     PDOpenSearchConfiguration.getAWSServiceName () +
                     "'");

        return new AwsSdk2Transport (software.amazon.awssdk.http.apache.ApacheHttpClient.builder ().build (),
                                     sHost,
                                     PDOpenSearchConfiguration.getAWSServiceName (),
                                     software.amazon.awssdk.regions.Region.of (sRegion),
                                     AwsSdk2TransportOptions.builder ().build ());
      }
      case NONE:
      {
        LOGGER.info ("Configuring OpenSearch for unauthenticated mode on '" + sEndpointURL + "'");

        return ApacheHttpClient5TransportBuilder.builder (HttpHost.create (URI.create (sEndpointURL))).build ();
      }
      default:
        throw new IllegalStateException ("Unsupported OpenSearch authentication type " + eAuthType);
    }
  }

  /**
   * @return The type mapping of the Peppol Directory index. The tokenization of each field is
   *         identical to the one used by the Apache Lucene based index.
   */
  @NonNull
  public static TypeMapping createTypeMapping ()
  {
    final ICommonsOrderedMap <String, Property> aProps = new CommonsLinkedHashMap <> ();

    // All the String based fields
    _addStringProperty (aProps, PDField.PARTICIPANT_ID.getFieldName (), EPDIndexFieldTokenize.NO_TOKENIZE);
    _addStringProperty (aProps, PDField.DOCTYPE_ID.getFieldName (), EPDIndexFieldTokenize.NO_TOKENIZE);
    _addStringProperty (aProps, PDField.REGISTRATION_DATE.getFieldName (), EPDIndexFieldTokenize.NO_TOKENIZE);
    _addStringProperty (aProps, PDField.NAME.getFieldName (), EPDIndexFieldTokenize.TOKENIZE);
    _addStringProperty (aProps, PDField.ML_NAME.getFieldName (), EPDIndexFieldTokenize.TOKENIZE);
    _addStringProperty (aProps, PDField.ML_LANGUAGE.getFieldName (), EPDIndexFieldTokenize.NO_TOKENIZE);
    _addStringProperty (aProps, PDField.COUNTRY_CODE.getFieldName (), EPDIndexFieldTokenize.NO_TOKENIZE);
    _addStringProperty (aProps, PDField.GEO_INFO.getFieldName (), EPDIndexFieldTokenize.TOKENIZE);
    _addStringProperty (aProps, PDField.IDENTIFIER_SCHEME.getFieldName (), EPDIndexFieldTokenize.TOKENIZE);
    _addStringProperty (aProps, PDField.IDENTIFIER_VALUE.getFieldName (), EPDIndexFieldTokenize.TOKENIZE);
    _addStringProperty (aProps, PDField.WEBSITE_URI.getFieldName (), EPDIndexFieldTokenize.TOKENIZE);
    _addStringProperty (aProps, PDField.CONTACT_TYPE.getFieldName (), EPDIndexFieldTokenize.TOKENIZE);
    _addStringProperty (aProps, PDField.CONTACT_NAME.getFieldName (), EPDIndexFieldTokenize.TOKENIZE);
    _addStringProperty (aProps, PDField.CONTACT_PHONE.getFieldName (), EPDIndexFieldTokenize.TOKENIZE);
    _addStringProperty (aProps, PDField.CONTACT_EMAIL.getFieldName (), EPDIndexFieldTokenize.TOKENIZE);
    _addStringProperty (aProps, PDField.ADDITIONAL_INFO.getFieldName (), EPDIndexFieldTokenize.TOKENIZE);
    _addStringProperty (aProps, PDField.METADATA_OWNERID.getFieldName (), EPDIndexFieldTokenize.NO_TOKENIZE);
    _addStringProperty (aProps, PDField.METADATA_REQUESTING_HOST.getFieldName (), EPDIndexFieldTokenize.NO_TOKENIZE);
    // The catch all field is tokenized but never read back
    _addStringProperty (aProps, CPDStorage.FIELD_ALL_FIELDS, EPDIndexFieldTokenize.TOKENIZE);

    // The only numeric field - it is never queried, so it needs no index
    aProps.put (PDField.METADATA_CREATIONDT.getFieldName (),
                Property.of (p -> p.long_ (l -> l.index (Boolean.FALSE).docValues (Boolean.FALSE))));

    return TypeMapping.builder ()
                      .properties (aProps)
                      // Everything that is not mapped above is not indexed at all
                      .dynamic (org.opensearch.client.opensearch._types.mapping.DynamicMapping.False)
                      // The catch all field is indexed but not stored
                      .source (s -> s.excludes (CPDStorage.FIELD_ALL_FIELDS))
                      .build ();
  }

  private static void _addStringProperty (@NonNull final Map <String, Property> aProps,
                                          @NonNull @Nonempty final String sFieldName,
                                          @NonNull final EPDIndexFieldTokenize eTokenize)
  {
    if (eTokenize.isTokenize ())
      aProps.put (sFieldName, Property.of (p -> p.text (t -> t.analyzer (ANALYZER_STANDARD))));
    else
    {
      // No "ignore_above" - the Peppol document type IDs are way longer than 256 characters
      aProps.put (sFieldName, Property.of (p -> p.keyword (k -> k)));
    }
  }

  private void _createIndexIfNotExisting () throws IOException
  {
    if (m_aClient.indices ().exists (e -> e.index (m_sIndexName)).value ())
    {
      LOGGER.info ("The OpenSearch index '" + m_sIndexName + "' already exists");
      return;
    }

    LOGGER.info ("Creating the OpenSearch index '" + m_sIndexName + "'");

    final IndexSettings aSettings = IndexSettings.builder ()
                                                 .numberOfShards (Integer.valueOf (PDOpenSearchConfiguration.getIndexShards ()))
                                                 .numberOfReplicas (Integer.valueOf (PDOpenSearchConfiguration.getIndexReplicas ()))
                                                 .build ();
    m_aClient.indices ().create (c -> c.index (m_sIndexName).settings (aSettings).mappings (createTypeMapping ()));

    LOGGER.info ("Successfully created the OpenSearch index '" + m_sIndexName + "'");
  }

  public void close () throws IOException
  {
    // Avoid double closing
    if (!m_aClosing.getAndSet (true))
    {
      m_aTransport.close ();
      LOGGER.info ("Closed the OpenSearch transport");
    }
  }

  public boolean isClosing ()
  {
    return m_aClosing.get ();
  }

  private void _checkClosing ()
  {
    if (isClosing ())
      throw new IllegalStateException ("The OpenSearch index is shutting down so no access is possible");
  }

  /**
   * @return The name of the OpenSearch index this object works on. Neither <code>null</code> nor
   *         empty.
   */
  @NonNull
  @Nonempty
  public final String getIndexName ()
  {
    return m_sIndexName;
  }

  @NonNull
  public ICommonsOrderedMap <String, String> getIndexInformation () throws IOException
  {
    _checkClosing ();

    final ICommonsOrderedMap <String, String> ret = new CommonsLinkedHashMap <> ();
    if (StringHelper.isNotEmpty (m_sEndpointURL))
      ret.put ("OpenSearch endpoint", m_sEndpointURL);
    ret.put ("OpenSearch index name", m_sIndexName);

    final InfoResponse aInfo = m_aClient.info ();
    ret.put ("Cluster name", aInfo.clusterName ());
    if (aInfo.version () != null)
    {
      ret.put ("Distribution", aInfo.version ().distribution ());
      ret.put ("Version", aInfo.version ().number ());
      ret.put ("Lucene version", aInfo.version ().luceneVersion ());
    }

    final CountResponse aCount = m_aClient.count (c -> c.index (m_sIndexName));
    ret.put ("Document count", Long.toString (aCount.count ()));
    return ret;
  }

  @NonNull
  public ICommonsList <String> getSplitIntoTerms (@NonNull @Nonempty final String sFieldName,
                                                  @NonNull @Nonempty final String sQueryString) throws IOException
  {
    ValueEnforcer.notEmpty (sFieldName, "FieldName");
    ValueEnforcer.notEmpty (sQueryString, "QueryString");
    _checkClosing ();

    // Use the analyzer of the field to split the query string into terms
    final AnalyzeResponse aResponse = m_aClient.indices ()
                                               .analyze (a -> a.index (m_sIndexName)
                                                               .field (sFieldName)
                                                               .text (sQueryString));
    final ICommonsList <String> ret = new CommonsArrayList <> ();
    if (aResponse.tokens () != null)
      for (final AnalyzeToken aToken : aResponse.tokens ())
        ret.add (aToken.token ());
    return ret;
  }

  public void updateDocuments (@Nullable final PDIndexQueryTerm aDeleteQuery,
                               @NonNull final List <PDIndexDocument> aDocs) throws IOException
  {
    ValueEnforcer.notNull (aDocs, "Docs");
    _checkClosing ();

    // OpenSearch has no atomic "delete and add" - delete first, add afterwards
    if (aDeleteQuery != null)
      _deleteByQuery (_toOpenSearchQuery (aDeleteQuery));

    if (!aDocs.isEmpty ())
    {
      final BulkRequest.Builder aBuilder = new BulkRequest.Builder ().index (m_sIndexName)
                                                                     // Make the changes visible
                                                                     // immediately
                                                                     .refresh (Refresh.True);
      for (final PDIndexDocument aDoc : aDocs)
      {
        // The document ID is created by OpenSearch, because all deletions happen by query
        final JsonData aJson = JsonData.of (_toJsonObject (aDoc));
        aBuilder.operations (o -> o.index (i -> i.document (aJson)));
      }

      final BulkResponse aResponse = m_aClient.bulk (aBuilder.build ());
      if (aResponse.errors ())
      {
        final StringBuilder aSB = new StringBuilder ();
        for (final BulkResponseItem aItem : aResponse.items ())
          if (aItem.error () != null)
          {
            if (aSB.length () > 0)
              aSB.append ("; ");
            aSB.append (aItem.error ().reason ());
          }
        throw new IOException ("Failed to add " +
                               aDocs.size () +
                               " documents to the OpenSearch index '" +
                               m_sIndexName +
                               "': " +
                               aSB);
      }

      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Added " + aDocs.size () + " documents to the OpenSearch index '" + m_sIndexName + "'");
    }
  }

  public void deleteDocuments (@NonNull final IPDIndexQuery aQuery) throws IOException
  {
    ValueEnforcer.notNull (aQuery, "Query");
    _checkClosing ();

    _deleteByQuery (_toOpenSearchQuery (aQuery));
  }

  private void _deleteByQuery (@NonNull final Query aQuery) throws IOException
  {
    final DeleteByQueryResponse aResponse = m_aClient.deleteByQuery (d -> d.index (m_sIndexName)
                                                                           .query (aQuery)
                                                                           // Make the changes
                                                                           // visible immediately
                                                                           .refresh (Refresh.True)
                                                                           // Concurrent updates
                                                                           // must not abort the
                                                                           // deletion
                                                                           .conflicts (Conflicts.Proceed));
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Deleted " + aResponse.deleted () + " documents from the OpenSearch index '" + m_sIndexName + "'");
  }

  @CheckForSigned
  public int getCount (@NonNull final IPDIndexQuery aQuery) throws IOException
  {
    ValueEnforcer.notNull (aQuery, "Query");
    _checkClosing ();

    final Query aOSQuery = _toOpenSearchQuery (aQuery);
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Counting in OpenSearch: " + aQuery);

    final CountResponse aResponse = m_aClient.count (c -> c.index (m_sIndexName).query (aOSQuery));
    final long nCount = aResponse.count ();
    return nCount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) nCount;
  }

  @CheckForSigned
  public int searchAll (@NonNull final IPDIndexQuery aQuery,
                        @CheckForSigned final int nMaxResultCount,
                        @NonNull final Consumer <? super PDIndexDocument> aConsumer) throws IOException
  {
    ValueEnforcer.notNull (aQuery, "Query");
    ValueEnforcer.notNull (aConsumer, "Consumer");
    _checkClosing ();

    final Query aOSQuery = _toOpenSearchQuery (aQuery);
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Searching OpenSearch: " + aQuery);

    if (nMaxResultCount > 0)
    {
      /*
       * Search top docs only. By default the total hit count is only tracked up to 10.000
       * documents, so it must be enabled explicitly to receive an exact number, that is the same a
       * separate "getCount" call would deliver.
       */
      final SearchResponse <JsonData> aResponse = m_aClient.search (s -> s.index (m_sIndexName)
                                                                          .query (aOSQuery)
                                                                          .trackTotalHits (t -> t.enabled (Boolean.TRUE))
                                                                          .size (Integer.valueOf (nMaxResultCount)),
                                                                    JsonData.class);
      _consumeHits (aResponse, aConsumer);

      final TotalHits aTotalHits = aResponse.hits ().total ();
      if (aTotalHits == null)
      {
        // Should not happen, as the total hit count tracking is enabled above
        LOGGER.warn ("The OpenSearch response of query " + aQuery + " contains no total hit count");
        return aResponse.hits ().hits ().size ();
      }
      final long nTotalHits = aTotalHits.value ();
      return nTotalHits > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) nTotalHits;
    }

    // Search all - use the scroll API, because a single search is limited to
    // "index.max_result_window" (10.000 by default) documents
    final Time aScrollTime = Time.of (t -> t.time (PDOpenSearchConfiguration.getScrollTimeoutMinutes () + "m"));
    final Integer aPageSize = Integer.valueOf (PDOpenSearchConfiguration.getScrollPageSize ());

    SearchResponse <JsonData> aResponse = m_aClient.search (s -> s.index (m_sIndexName)
                                                                  .query (aOSQuery)
                                                                  .size (aPageSize)
                                                                  .scroll (aScrollTime)
                                                                  // Sorting by document order is
                                                                  // the most efficient way to
                                                                  // scroll
                                                                  .sort (SortOptions.of (x -> x.doc (d -> d))),
                                                            JsonData.class);
    String sScrollID = aResponse.scrollId ();
    // Every matching document is scrolled through, so counting them is the total hit count
    int nTotalHitCount = 0;
    try
    {
      while (!aResponse.hits ().hits ().isEmpty ())
      {
        nTotalHitCount += aResponse.hits ().hits ().size ();
        _consumeHits (aResponse, aConsumer);

        final String sCurScrollID = sScrollID;
        aResponse = m_aClient.scroll (s -> s.scrollId (sCurScrollID).scroll (aScrollTime), JsonData.class);
        sScrollID = aResponse.scrollId ();
      }
    }
    finally
    {
      if (StringHelper.isNotEmpty (sScrollID))
      {
        final String sCurScrollID = sScrollID;
        try
        {
          m_aClient.clearScroll (c -> c.scrollId (sCurScrollID));
        }
        catch (final IOException | RuntimeException ex)
        {
          // Not fatal - the scroll context times out anyway
          LOGGER.warn ("Failed to clear the OpenSearch scroll context: " + ex.getMessage ());
        }
      }
    }
    return nTotalHitCount;
  }

  private static void _consumeHits (@NonNull final SearchResponse <JsonData> aResponse,
                                    @NonNull final Consumer <? super PDIndexDocument> aConsumer)
  {
    for (final Hit <JsonData> aHit : aResponse.hits ().hits ())
    {
      final JsonData aSource = aHit.source ();
      if (aSource == null)
      {
        LOGGER.warn ("The OpenSearch hit with ID '" + aHit.id () + "' contains no source");
        continue;
      }

      final JsonValue aValue = aSource.toJson ();
      if (aValue instanceof final JsonObject aJsonObject)
        aConsumer.accept (_toIndexDocument (aJsonObject));
      else
        LOGGER.warn ("The source of the OpenSearch hit with ID '" + aHit.id () + "' is not a JSON object");
    }
  }

  @NonNull
  private static Query _toOpenSearchQuery (@NonNull final IPDIndexQuery aQuery)
  {
    // Reuse the previously translated query - more than one search engine may be active at the
    // same time, so the type must be checked
    final Object aNativeQuery = aQuery.getNativeQuery ();
    if (aNativeQuery instanceof final Query aOSQuery)
      return aOSQuery;

    final Query ret = _recursiveCreateOpenSearchQuery (aQuery);
    aQuery.setNativeQuery (ret);
    return ret;
  }

  @NonNull
  private static Query _recursiveCreateOpenSearchQuery (@NonNull final IPDIndexQuery aQuery)
  {
    // IPDIndexQuery is sealed and this switch has deliberately no "default" case, so that adding a
    // new query type is a compile error here
    return switch (aQuery)
    {
      case final PDIndexQueryMatchAll _ -> Query.of (q -> q.matchAll (m -> m));
      case final PDIndexQueryTerm aTermQuery -> Query.of (q -> q.term (t -> t.field (aTermQuery.getFieldName ())
                                                                             .value (FieldValue.of (aTermQuery.getValue ()))));
      case final PDIndexQueryPrefix aPrefixQuery ->
      {
        // Note: the prefix query is supposed to work with the exact term, without a trailing "*"
        yield Query.of (q -> q.prefix (p -> p.field (aPrefixQuery.getFieldName ()).value (aPrefixQuery.getValue ())));
      }
      case final PDIndexQueryContains aContainsQuery ->
      {
        // This works -> text ==> *text*
        yield Query.of (q -> q.wildcard (w -> w.field (aContainsQuery.getFieldName ())
                                               .value ("*" + aContainsQuery.getValue () + "*")));
      }
      case final PDIndexQueryBool aBoolQuery ->
      {
        final BoolQuery.Builder aBuilder = new BoolQuery.Builder ();
        for (final PDIndexQueryBool.Clause aClause : aBoolQuery.getAllClauses ())
        {
          final Query aClauseQuery = _toOpenSearchQuery (aClause.getQuery ());
          // Deliberately no "default" case, so that adding a new occurrence is a compile error
          // here. This must be a switch expression, because a switch statement over an enum is not
          // checked for exhaustiveness. The result is the builder itself and is not needed
          final var _ = switch (aClause.getOccur ())
          {
            case MUST -> aBuilder.must (aClauseQuery);
            case SHOULD -> aBuilder.should (aClauseQuery);
            case FILTER -> aBuilder.filter (aClauseQuery);
          };
        }
        yield Query.of (q -> q.bool (aBuilder.build ()));
      }
    };
  }

  @NonNull
  private static JsonObject _toJsonObject (@NonNull final PDIndexDocument aDoc)
  {
    // Group all values by field name, retaining the order in which they were added
    final ICommonsOrderedMap <String, ICommonsList <PDIndexField>> aGrouped = new CommonsLinkedHashMap <> ();
    for (final PDIndexField aField : aDoc.fields ())
      aGrouped.computeIfAbsent (aField.getName (), _ -> new CommonsArrayList <> ()).add (aField);

    final JsonObjectBuilder aObjBuilder = Json.createObjectBuilder ();
    for (final Map.Entry <String, ICommonsList <PDIndexField>> aEntry : aGrouped.entrySet ())
    {
      final ICommonsList <PDIndexField> aFields = aEntry.getValue ();
      if (aFields.size () == 1)
        _addSingleValue (aObjBuilder, aEntry.getKey (), aFields.getFirstOrNull ());
      else
      {
        final JsonArrayBuilder aArrayBuilder = Json.createArrayBuilder ();
        for (final PDIndexField aField : aFields)
          if (aField.isNumeric ())
            aArrayBuilder.add (aField.getNumericValue ().longValue ());
          else
            aArrayBuilder.add (aField.getStringValue ());
        aObjBuilder.add (aEntry.getKey (), aArrayBuilder);
      }
    }
    return aObjBuilder.build ();
  }

  private static void _addSingleValue (@NonNull final JsonObjectBuilder aObjBuilder,
                                       @NonNull @Nonempty final String sFieldName,
                                       @NonNull final PDIndexField aField)
  {
    if (aField.isNumeric ())
      aObjBuilder.add (sFieldName, aField.getNumericValue ().longValue ());
    else
      aObjBuilder.add (sFieldName, aField.getStringValue ());
  }

  @NonNull
  private static PDIndexDocument _toIndexDocument (@NonNull final JsonObject aSource)
  {
    final PDIndexDocument ret = new PDIndexDocument (aSource.size ());
    for (final Map.Entry <String, JsonValue> aEntry : aSource.entrySet ())
    {
      final String sFieldName = aEntry.getKey ();
      final JsonValue aValue = aEntry.getValue ();
      if (aValue instanceof final JsonArray aJsonArray)
      {
        for (final JsonValue aChild : aJsonArray)
          _addFieldValue (ret, sFieldName, aChild);
      }
      else
        _addFieldValue (ret, sFieldName, aValue);
    }
    return ret;
  }

  private static void _addFieldValue (@NonNull final PDIndexDocument aDoc,
                                      @NonNull @Nonempty final String sFieldName,
                                      @NonNull final JsonValue aValue)
  {
    if (aValue instanceof final JsonString aJsonString)
      aDoc.add (PDIndexField.createStoredString (sFieldName, aJsonString.getString ()));
    else
      if (aValue instanceof final JsonNumber aJsonNumber)
        aDoc.add (PDIndexField.createNumeric (sFieldName, Long.valueOf (aJsonNumber.longValue ())));
      else
        LOGGER.warn ("Ignoring the value of OpenSearch field '" +
                     sFieldName +
                     "' because it is neither a String nor a Number");
  }
}
