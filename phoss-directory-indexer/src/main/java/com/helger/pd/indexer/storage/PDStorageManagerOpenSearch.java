package com.helger.pd.indexer.storage;

import java.io.IOException;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.Result;
import org.opensearch.client.opensearch._types.query_dsl.FieldAndFormat;
import org.opensearch.client.opensearch._types.query_dsl.IdsQuery;
import org.opensearch.client.opensearch._types.query_dsl.MatchAllQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.QueryStringQuery;
import org.opensearch.client.opensearch.core.CountResponse;
import org.opensearch.client.opensearch.core.DeleteResponse;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.IndexResponse;
import org.opensearch.client.opensearch.core.InfoResponse;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.CheckForSigned;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.state.ESuccess;
import com.helger.collection.commons.CommonsTreeSet;
import com.helger.collection.commons.ICommonsSortedSet;
import com.helger.httpclient.HttpClientFactory;
import com.helger.httpclient.HttpClientSettings;
import com.helger.pd.indexer.businesscard.PDExtendedBusinessCard;
import com.helger.pd.indexer.mgr.IPDStorageManager;
import com.helger.pd.indexer.storage.model.PDStoredMetaData;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.photon.audit.AuditHelper;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * {@link IPDStorageManager} implementation using OpenSearch Java client
 * 
 * @author Philip Helger
 */
public class PDStorageManagerOpenSearch implements IPDStorageManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PDStorageManagerOpenSearch.class);
  private static final String INDEX_BUSINESS_CARD = "bc";

  private final OpenSearchTransport m_aTransport;
  private final OpenSearchClient m_aClient;

  public PDStorageManagerOpenSearch () throws OpenSearchException, IOException
  {
    // TODO make customizable
    final HttpHost aOSHost = new HttpHost ("https", "localhost", 9200);

    // TODO make customizable???
    final HttpClientSettings aHCS = new HttpClientSettings ();
    final HttpClientFactory aHCF = new HttpClientFactory (aHCS)
    {
      @Override
      public CredentialsProvider createCredentialsProvider ()
      {
        BasicCredentialsProvider aBCP = (BasicCredentialsProvider) super.createCredentialsProvider ();
        if (aBCP != null)
          aBCP = new BasicCredentialsProvider ();

        // TODO make customizable
        aBCP.setCredentials (new AuthScope (aOSHost),
                             new UsernamePasswordCredentials ("admin", "12PASSword!!".toCharArray ()));
        return aBCP;
      }
    };

    m_aTransport = ApacheHttpClient5TransportBuilder.builder (aOSHost).setHttpClientConfigCallback (hc -> {
      aHCF.applyTo (hc);
      return hc;
    }).build ();
    m_aClient = new OpenSearchClient (m_aTransport);

    final InfoResponse info = m_aClient.info ();
    LOGGER.info ("Startup info: " + info.version ().distribution () + ": " + info.version ().number ());
  }

  public void close () throws IOException
  {
    m_aTransport.close ();
  }

  @Nonnull
  public ESuccess createOrUpdateEntry (@Nonnull final IParticipantIdentifier aParticipantID,
                                       @Nonnull final PDExtendedBusinessCard aExtBI,
                                       @Nonnull final PDStoredMetaData aMetaData) throws IOException
  {
    final OpenSearchIndexData aData = new OpenSearchIndexData (aExtBI, aMetaData);

    final IndexResponse aResponse = m_aClient.index (new IndexRequest.Builder <OpenSearchIndexData> ().index (INDEX_BUSINESS_CARD)
                                                                                                      .id (aData.getID ())
                                                                                                      .document (aData)
                                                                                                      .build ());
    final ESuccess ret = switch (aResponse.result ())
    {
      case Created:
      case Updated:
        yield ESuccess.SUCCESS;
      default:
        yield ESuccess.FAILURE;
    };
    if (ret.isSuccess ())
      AuditHelper.onAuditExecuteSuccess ("pd-indexer-create",
                                         aData.getID (),
                                         Integer.valueOf (aExtBI.getDocumentTypeCount ()),
                                         aMetaData);
    else
      AuditHelper.onAuditExecuteFailure ("pd-indexer-create",
                                         aData.getID (),
                                         Integer.valueOf (aExtBI.getDocumentTypeCount ()),
                                         aMetaData);
    return ret;
  }

  @CheckForSigned
  public long deleteEntry (@Nonnull final IParticipantIdentifier aParticipantID,
                           @Nullable final PDStoredMetaData aMetaData,
                           final boolean bVerifyOwner) throws IOException
  {
    final String sID = aParticipantID.getURIEncoded ();

    // Query the existing record
    final SearchResponse <OpenSearchIndexData> searchResponse = m_aClient.search (s -> s.index (INDEX_BUSINESS_CARD)
                                                                                        .query (new Query.Builder ().ids (new IdsQuery.Builder ().values (sID)
                                                                                                                                                 .build ())
                                                                                                                    .build ()),
                                                                                  OpenSearchIndexData.class);
    if (searchResponse.hits ().hits ().isEmpty ())
    {
      LOGGER.info ("Found no document to delete");
      return 0;
    }

    if (searchResponse.hits ().hits ().size () > 1)
      LOGGER.error ("Found more than one hit (" + searchResponse.hits ().hits ().size () + ") for deletion");

    final Hit <OpenSearchIndexData> aData = searchResponse.hits ().hits ().get (0);
    final String sStoredOwnerID = aData.source ().getMetaData ().getOwnerID ();
    if (sStoredOwnerID == null)
      throw new IllegalStateException ("No owner ID is present");
    if (!sStoredOwnerID.equals (aMetaData.getOwnerID ()) &&
        !sStoredOwnerID.equals (PDStoredMetaData.getOwnerIDSeatNumber (aMetaData.getOwnerID ())) &&
        !CPDStorage.isSpecialOwnerID (sStoredOwnerID))
    {
      LOGGER.info ("The document to be deleted has the wrong owner: '" +
                   sStoredOwnerID +
                   "' - requested owner ID is '" +
                   aMetaData.getOwnerID () +
                   "'");
      return 0;
    }

    // Safe to delete the document
    final DeleteResponse aResponse = m_aClient.delete (b -> b.index (INDEX_BUSINESS_CARD).id (sID));
    return aResponse.result () == Result.Deleted ? 1 : 0;
  }

  private static Query _getStringQuery (@Nonnull final org.apache.lucene.search.Query aQuery)
  {
    return new Query.Builder ().queryString (new QueryStringQuery.Builder ().query (aQuery.toString ()).build ())
                               .build ();
  }

  @CheckForSigned
  public long getCount (@Nonnull final org.apache.lucene.search.Query aQuery)
  {
    try
    {
      // Query the existing record
      final CountResponse countResponse = m_aClient.count (s -> s.index (INDEX_BUSINESS_CARD)
                                                                 .query (_getStringQuery (aQuery)));
      return countResponse.count ();
    }
    catch (IOException | OpenSearchException ex)
    {
      LOGGER.error ("Error counting documents with query " + aQuery, ex);
      return -1;
    }
  }

  @CheckForSigned
  public long getContainedParticipantCount ()
  {
    try
    {
      // Query the existing record
      final CountResponse countResponse = m_aClient.count (s -> s.index (INDEX_BUSINESS_CARD));
      return countResponse.count ();
    }
    catch (IOException | OpenSearchException ex)
    {
      LOGGER.error ("Error counting participants", ex);
      return -1;
    }
  }

  public boolean containsEntry (@Nonnull final IParticipantIdentifier aPI)
  {
    try
    {
      // Query the existing record
      final CountResponse countResponse = m_aClient.count (s -> s.index (INDEX_BUSINESS_CARD)
                                                                 .query (new Query.Builder ().ids (new IdsQuery.Builder ().values (aPI.getURIEncoded ())
                                                                                                                          .build ())
                                                                                             .build ()));
      return countResponse.count () > 0;
    }
    catch (final IOException | OpenSearchException ex)
    {
      LOGGER.error ("Error in containsEntry " + aPI.getURIEncoded (), ex);
      return false;
    }
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsSortedSet <IParticipantIdentifier> getAllContainedParticipantIDs ()
  {
    try
    {
      final SearchResponse <OpenSearchIndexData> searchResponse = m_aClient.search (s -> s.index (INDEX_BUSINESS_CARD)
                                                                                          .fields (new FieldAndFormat.Builder ().field (OpenSearchIndexData.FIELD_PARTICIPANT)
                                                                                                                                .build ())
                                                                                          .query (new Query.Builder ().matchAll (new MatchAllQuery.Builder ().build ())
                                                                                                                      .build ()),
                                                                                    OpenSearchIndexData.class);

      final ICommonsSortedSet <IParticipantIdentifier> ret = new CommonsTreeSet <> ();
      // TODO
      return ret;
    }
    catch (final IOException | OpenSearchException ex)
    {
      LOGGER.error ("Error in getAllContainedParticipantIDs", ex);
      return new CommonsTreeSet <> ();
    }
  }
}
