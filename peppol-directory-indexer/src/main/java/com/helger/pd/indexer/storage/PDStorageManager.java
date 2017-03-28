/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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
package com.helger.pd.indexer.storage;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.ObjIntConsumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.callback.IThrowingRunnable;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.CommonsTreeSet;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.collection.ext.ICommonsSortedSet;
import com.helger.commons.collection.multimap.IMultiMapListBased;
import com.helger.commons.collection.multimap.MultiLinkedHashMapArrayListBased;
import com.helger.commons.function.IThrowingSupplier;
import com.helger.commons.state.ESuccess;
import com.helger.commons.statistics.IMutableStatisticsHandlerKeyedTimer;
import com.helger.commons.statistics.StatisticsManager;
import com.helger.commons.string.StringHelper;
import com.helger.commons.timing.StopWatch;
import com.helger.datetime.util.PDTWebDateHelper;
import com.helger.pd.businesscard.PDExtendedBusinessCard;
import com.helger.pd.businesscard.v1.PD1BusinessCardType;
import com.helger.pd.businesscard.v1.PD1BusinessEntityType;
import com.helger.pd.businesscard.v1.PD1ContactType;
import com.helger.pd.businesscard.v1.PD1IdentifierType;
import com.helger.pd.indexer.lucene.AllDocumentsCollector;
import com.helger.pd.indexer.lucene.PDLucene;
import com.helger.pd.indexer.mgr.IPDStorageManager;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.photon.basic.audit.AuditHelper;

/**
 * The global storage manager that wraps the used Lucene index.
 *
 * @author Philip Helger
 */
@Immutable
public final class PDStorageManager implements IPDStorageManager
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (PDStorageManager.class);
  private static final String FIELD_GROUP_END = "groupend";
  private static final FieldType TYPE_GROUP_END = new FieldType ();
  private static final String VALUE_GROUP_END = "x";
  private static final IMutableStatisticsHandlerKeyedTimer s_aStatsQueryTimer = StatisticsManager.getKeyedTimerHandler (PDStorageManager.class.getName () +
                                                                                                                        "$query");

  static
  {
    TYPE_GROUP_END.setStored (false);
    TYPE_GROUP_END.setIndexOptions (IndexOptions.DOCS);
    TYPE_GROUP_END.setOmitNorms (true);
    TYPE_GROUP_END.freeze ();
  }

  private final PDLucene m_aLucene;

  public PDStorageManager (@Nonnull final PDLucene aLucene)
  {
    m_aLucene = ValueEnforcer.notNull (aLucene, "Lucene");
  }

  public void close () throws IOException
  {
    m_aLucene.close ();
  }

  public boolean containsEntry (@Nullable final IParticipantIdentifier aParticipantID) throws IOException
  {
    if (aParticipantID == null)
      return false;

    final IThrowingSupplier <Boolean, IOException> cb = () -> {
      final IndexSearcher aSearcher = m_aLucene.getSearcher ();
      if (aSearcher != null)
      {
        // Search only documents that do not have the deleted field
        final Query aQuery = PDQueryManager.andNotDeleted (new TermQuery (PDField.PARTICIPANT_ID.getTerm (aParticipantID)));
        final TopDocs aTopDocs = _timedSearch ( () -> aSearcher.search (aQuery, 1), aQuery);
        if (aTopDocs.totalHits > 0)
          return Boolean.TRUE;
      }
      return Boolean.FALSE;
    };
    return m_aLucene.callAtomic (cb).booleanValue ();
  }

  private static void _timedSearch (@Nonnull final IThrowingRunnable <IOException> aRunnable,
                                    @Nonnull final Query aQuery) throws IOException
  {
    final StopWatch aSW = StopWatch.createdStarted ();
    try
    {
      aRunnable.run ();
    }
    finally
    {
      final long nMillis = aSW.stopAndGetMillis ();
      s_aStatsQueryTimer.addTime (aQuery.toString (), nMillis);
    }
  }

  private static <T> T _timedSearch (@Nonnull final IThrowingSupplier <T, IOException> aRunnable,
                                     @Nonnull final Query aQuery) throws IOException
  {
    final StopWatch aSW = StopWatch.createdStarted ();
    try
    {
      return aRunnable.get ();
    }
    finally
    {
      final long nMillis = aSW.stopAndGetMillis ();
      s_aStatsQueryTimer.addTime (aQuery.toString (), nMillis);
    }
  }

  @Nonnull
  public ESuccess deleteEntry (@Nonnull final IParticipantIdentifier aParticipantID,
                               @Nonnull final PDDocumentMetaData aMetaData) throws IOException
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    ValueEnforcer.notNull (aMetaData, "MetaData");

    return m_aLucene.runAtomic ( () -> {
      final ICommonsList <Document> aDocuments = new CommonsArrayList<> ();

      // Get all documents to be marked as deleted
      final IndexSearcher aSearcher = m_aLucene.getSearcher ();
      if (aSearcher != null)
      {
        // Main searching
        final Query aQuery = new TermQuery (PDField.PARTICIPANT_ID.getTerm (aParticipantID));
        _timedSearch ( () -> aSearcher.search (aQuery,
                                               new AllDocumentsCollector (m_aLucene,
                                                                          (aDoc, nDocID) -> aDocuments.add (aDoc))),
                       aQuery);
      }

      if (!aDocuments.isEmpty ())
      {
        // Mark document as deleted
        aDocuments.forEach (aDocument -> aDocument.add (new IntPoint (CPDStorage.FIELD_DELETED, 1)));

        // Update the documents
        m_aLucene.updateDocuments (PDField.PARTICIPANT_ID.getTerm (aParticipantID), aDocuments);
      }

      s_aLogger.info ("Marked " + aDocuments.size () + " Lucene documents as deleted");
      AuditHelper.onAuditExecuteSuccess ("pd-indexer-delete",
                                         aParticipantID.getURIEncoded (),
                                         Integer.valueOf (aDocuments.size ()),
                                         aMetaData);
    });
  }

  @Nonnull
  public ESuccess createOrUpdateEntry (@Nonnull final IParticipantIdentifier aParticipantID,
                                       @Nonnull final PDExtendedBusinessCard aExtBI,
                                       @Nonnull final PDDocumentMetaData aMetaData) throws IOException
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    ValueEnforcer.notNull (aExtBI, "ExtBI");
    ValueEnforcer.notNull (aMetaData, "MetaData");

    return m_aLucene.runAtomic ( () -> {
      final ICommonsList <Document> aDocs = new CommonsArrayList<> ();

      final PD1BusinessCardType aBI = aExtBI.getBusinessCard ();
      for (final PD1BusinessEntityType aBusinessEntity : aBI.getBusinessEntity ())
      {
        // Convert entity to Lucene document
        final Document aDoc = new Document ();
        final StringBuilder aSBAllFields = new StringBuilder ();

        aDoc.add (PDField.PARTICIPANT_ID.getAsField (aParticipantID));
        aSBAllFields.append (PDField.PARTICIPANT_ID.getAsStorageValue (aParticipantID)).append (' ');

        if (aBusinessEntity.getName () != null)
        {
          aDoc.add (PDField.NAME.getAsField (aBusinessEntity.getName ()));
          aSBAllFields.append (aBusinessEntity.getName ()).append (' ');
        }

        if (aBusinessEntity.getCountryCode () != null)
        {
          aDoc.add (PDField.COUNTRY_CODE.getAsField (aBusinessEntity.getCountryCode ()));
          aSBAllFields.append (aBusinessEntity.getCountryCode ()).append (' ');
        }

        // Add all document types to all documents
        for (final IDocumentTypeIdentifier aDocTypeID : aExtBI.getAllDocumentTypeIDs ())
        {
          aDoc.add (PDField.DOCTYPE_ID.getAsField (aDocTypeID));
          aSBAllFields.append (PDField.DOCTYPE_ID.getAsStorageValue (aDocTypeID)).append (' ');
        }

        if (aBusinessEntity.getGeographicalInformation () != null)
        {
          aDoc.add (PDField.GEO_INFO.getAsField (aBusinessEntity.getGeographicalInformation ()));
          aSBAllFields.append (aBusinessEntity.getGeographicalInformation ()).append (' ');
        }

        for (final PD1IdentifierType aIdentifier : aBusinessEntity.getIdentifier ())
        {
          aDoc.add (PDField.IDENTIFIER_SCHEME.getAsField (aIdentifier.getScheme ()));
          aSBAllFields.append (aIdentifier.getScheme ()).append (' ');

          aDoc.add (PDField.IDENTIFIER_VALUE.getAsField (aIdentifier.getValue ()));
          aSBAllFields.append (aIdentifier.getValue ()).append (' ');
        }

        for (final String sWebSite : aBusinessEntity.getWebsiteURI ())
        {
          aDoc.add (PDField.WEBSITE_URI.getAsField (sWebSite));
          aSBAllFields.append (sWebSite).append (' ');
        }

        for (final PD1ContactType aContact : aBusinessEntity.getContact ())
        {
          final String sType = StringHelper.getNotNull (aContact.getType ());
          aDoc.add (PDField.CONTACT_TYPE.getAsField (sType));
          aSBAllFields.append (sType).append (' ');

          final String sName = StringHelper.getNotNull (aContact.getName ());
          aDoc.add (PDField.CONTACT_NAME.getAsField (sName));
          aSBAllFields.append (sName).append (' ');

          final String sPhone = StringHelper.getNotNull (aContact.getPhoneNumber ());
          aDoc.add (PDField.CONTACT_PHONE.getAsField (sPhone));
          aSBAllFields.append (sPhone).append (' ');

          final String sEmail = StringHelper.getNotNull (aContact.getEmail ());
          aDoc.add (PDField.CONTACT_EMAIL.getAsField (sEmail));
          aSBAllFields.append (sEmail).append (' ');
        }

        if (aBusinessEntity.getAdditionalInformation () != null)
        {
          aDoc.add (PDField.ADDITIONAL_INFO.getAsField (aBusinessEntity.getAdditionalInformation ()));
          aSBAllFields.append (aBusinessEntity.getAdditionalInformation ()).append (' ');
        }

        if (aBusinessEntity.getRegistrationDate () != null)
        {
          final String sDate = PDTWebDateHelper.getAsStringXSD (aBusinessEntity.getRegistrationDate ());
          aDoc.add (PDField.REGISTRATION_DATE.getAsField (sDate));
          aSBAllFields.append (sDate).append (' ');
        }

        // Add the "all" field - no need to store
        aDoc.add (new TextField (CPDStorage.FIELD_ALL_FIELDS, aSBAllFields.toString (), Store.NO));

        // Add meta data (not part of the "all field" field!)
        // Lucene6: cannot yet use a LongPoint because it has no way to create a
        // stored one
        aDoc.add (PDField.METADATA_CREATIONDT.getAsField (aMetaData.getCreationDT ()));
        aDoc.add (PDField.METADATA_OWNERID.getAsField (aMetaData.getOwnerID ()));
        aDoc.add (PDField.METADATA_REQUESTING_HOST.getAsField (aMetaData.getRequestingHost ()));

        aDocs.add (aDoc);
      }

      if (aDocs.isNotEmpty ())
      {
        // Add "group end" marker
        CollectionHelper.getLastElement (aDocs).add (new Field (FIELD_GROUP_END, VALUE_GROUP_END, TYPE_GROUP_END));
      }

      // Delete all existing documents of the participant ID
      // and add the new ones to the index
      m_aLucene.updateDocuments (PDField.PARTICIPANT_ID.getTerm (aParticipantID), aDocs);

      s_aLogger.info ("Added " + aDocs.size () + " Lucene documents");
      AuditHelper.onAuditExecuteSuccess ("pd-indexer-create",
                                         aParticipantID.getURIEncoded (),
                                         Integer.valueOf (aDocs.size ()),
                                         aMetaData);
    });
  }

  /**
   * Search all documents matching the passed query and pass the result on to
   * the provided {@link Consumer}.
   *
   * @param aQuery
   *        Query to execute. May not be <code>null</code>-
   * @param aCollector
   *        The Lucene collector to be used. May not be <code>null</code>.
   * @throws IOException
   *         On Lucene error
   * @see #getAllDocuments(Query)
   */
  public void searchAtomic (@Nonnull final Query aQuery, @Nonnull final Collector aCollector) throws IOException
  {
    ValueEnforcer.notNull (aQuery, "Query");
    ValueEnforcer.notNull (aCollector, "Collector");

    m_aLucene.runAtomic ( () -> {
      final IndexSearcher aSearcher = m_aLucene.getSearcher ();
      if (aSearcher != null)
      {
        if (s_aLogger.isDebugEnabled ())
          s_aLogger.debug ("Searching Lucene: " + aQuery);

        // Search all documents, collect them
        _timedSearch ( () -> aSearcher.search (aQuery, aCollector), aQuery);
      }
      else
        s_aLogger.error ("Failed to obtain IndexSearcher");
    });
  }

  /**
   * Search all documents matching the passed query and pass the result on to
   * the provided {@link Consumer}.
   *
   * @param aQuery
   *        Query to execute. May not be <code>null</code>-
   * @param aConsumer
   *        The consumer of the {@link PDStoredDocument} objects.
   * @throws IOException
   *         On Lucene error
   * @see #searchAtomic(Query, Collector)
   * @see #getAllDocuments(Query)
   */
  public void searchAllDocuments (@Nonnull final Query aQuery,
                                  @Nonnull final Consumer <PDStoredDocument> aConsumer) throws IOException
  {
    ValueEnforcer.notNull (aQuery, "Query");
    ValueEnforcer.notNull (aConsumer, "Consumer");

    final ObjIntConsumer <Document> aConverter = (aDoc, nDocID) -> aConsumer.accept (PDStoredDocument.create (aDoc));
    final Collector aCollector = new AllDocumentsCollector (m_aLucene, aConverter);
    searchAtomic (aQuery, aCollector);
  }

  /**
   * Get all {@link PDStoredDocument} objects matching the provided query. This
   * is a specialization of {@link #searchAllDocuments(Query, Consumer)}.
   *
   * @param aQuery
   *        The query to be executed. May not be <code>null</code>.
   * @return A non-<code>null</code> but maybe empty list of matching documents
   * @see #searchAllDocuments(Query, Consumer)
   */
  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <PDStoredDocument> getAllDocuments (@Nonnull final Query aQuery)
  {
    final ICommonsList <PDStoredDocument> aTargetList = new CommonsArrayList<> ();
    try
    {
      searchAllDocuments (aQuery, aDoc -> aTargetList.add (aDoc));
    }
    catch (final IOException ex)
    {
      s_aLogger.error ("Error searching for documents with query " + aQuery, ex);
    }
    return aTargetList;
  }

  @Nonnull
  public ICommonsList <PDStoredDocument> getAllDocumentsOfParticipant (@Nonnull final IParticipantIdentifier aParticipantID)
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    return getAllDocuments (new TermQuery (PDField.PARTICIPANT_ID.getTerm (aParticipantID)));
  }

  /**
   * Get all documents matching the passed country code
   *
   * @param sCountryCode
   *        Country code to search. May not be <code>null</code>.
   * @return Non-<code>null</code> but maybe empty list of documents
   */
  @Nonnull
  public ICommonsList <PDStoredDocument> getAllDocumentsOfCountryCode (@Nonnull final String sCountryCode)
  {
    ValueEnforcer.notNull (sCountryCode, "CountryCode");
    return getAllDocuments (new TermQuery (PDField.COUNTRY_CODE.getTerm (sCountryCode)));
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsSortedSet <IParticipantIdentifier> getAllContainedParticipantIDs ()
  {
    final ICommonsSortedSet <IParticipantIdentifier> aTargetSet = new CommonsTreeSet<> ();
    final Query aQuery = PDQueryManager.andNotDeleted (new MatchAllDocsQuery ());
    try
    {
      final ObjIntConsumer <Document> aConsumer = (aDoc,
                                                   nDocID) -> aTargetSet.add (PDField.PARTICIPANT_ID.getDocValue (aDoc));
      final Collector aCollector = new AllDocumentsCollector (m_aLucene, aConsumer);
      searchAtomic (aQuery, aCollector);
    }
    catch (final IOException ex)
    {
      s_aLogger.error ("Error searching for documents with query " + aQuery, ex);
    }
    return aTargetSet;
  }

  /**
   * Group the passed document list by participant ID
   *
   * @param aDocs
   *        The document list to group.
   * @return A non-<code>null</code> ordered map with the results. Order is like
   *         the order of the input list.
   */
  @Nonnull
  @ReturnsMutableCopy
  public static IMultiMapListBased <IParticipantIdentifier, PDStoredDocument> getGroupedByParticipantID (@Nonnull final List <PDStoredDocument> aDocs)
  {
    final MultiLinkedHashMapArrayListBased <IParticipantIdentifier, PDStoredDocument> ret = new MultiLinkedHashMapArrayListBased<> ();
    for (final PDStoredDocument aDoc : aDocs)
      ret.putSingle (aDoc.getParticipantID (), aDoc);
    return ret;
  }
}
