/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ObjIntConsumer;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.TotalHitCountCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.CheckForSigned;
import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.CGlobal;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.functional.IThrowingSupplier;
import com.helger.base.iface.IThrowingRunnable;
import com.helger.base.numeric.mutable.MutableInt;
import com.helger.base.state.ESuccess;
import com.helger.base.string.StringHelper;
import com.helger.base.timing.StopWatch;
import com.helger.collection.CollectionFind;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsLinkedHashMap;
import com.helger.collection.commons.CommonsTreeMap;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsMap;
import com.helger.collection.commons.ICommonsSortedMap;
import com.helger.datetime.web.PDTWebDateHelper;
import com.helger.pd.indexer.businesscard.PDExtendedBusinessCard;
import com.helger.pd.indexer.lucene.AllDocumentsCollector;
import com.helger.pd.indexer.lucene.PDLucene;
import com.helger.pd.indexer.mgr.IPDStorageManager;
import com.helger.pd.indexer.storage.field.PDField;
import com.helger.peppol.businesscard.generic.PDBusinessCard;
import com.helger.peppol.businesscard.generic.PDBusinessEntity;
import com.helger.peppol.businesscard.generic.PDContact;
import com.helger.peppol.businesscard.generic.PDIdentifier;
import com.helger.peppol.businesscard.generic.PDName;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.simple.participant.SimpleParticipantIdentifier;
import com.helger.photon.audit.AuditHelper;
import com.helger.statistics.api.IMutableStatisticsHandlerKeyedTimer;
import com.helger.statistics.impl.StatisticsManager;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * The global storage manager that wraps the used Lucene index.
 *
 * @author Philip Helger
 */
@Immutable
public final class PDStorageManager implements IPDStorageManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PDStorageManager.class);
  private static final String FIELD_GROUP_END = "groupend";
  private static final FieldType TYPE_GROUP_END = new FieldType ();
  private static final String VALUE_GROUP_END = "x";
  private static final IMutableStatisticsHandlerKeyedTimer STATS_QUERY_TIMER = StatisticsManager.getKeyedTimerHandler (PDStorageManager.class.getName () +
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

  private static void _timedSearch (@Nonnull final IThrowingRunnable <IOException> aRunnable,
                                    @Nonnull final Query aQuery) throws IOException
  {
    _timedSearch ( () -> {
      aRunnable.run ();
      return null;
    }, aQuery);
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
      STATS_QUERY_TIMER.addTime (aQuery.toString (), nMillis);
      if (nMillis > CGlobal.MILLISECONDS_PER_SECOND)
        LOGGER.warn ("Lucene Query " + aQuery + " took too long: " + nMillis + "ms");
    }
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
        final Query aQuery = new TermQuery (PDField.PARTICIPANT_ID.getExactMatchTerm (aParticipantID));
        final TopDocs aTopDocs = _timedSearch ( () -> aSearcher.search (aQuery, 1), aQuery);
        // Lucene 8
        if (aTopDocs.totalHits.value > 0)
          // Lucene 7
          // if (aTopDocs.totalHits > 0)
          return Boolean.TRUE;
      }
      return Boolean.FALSE;
    };
    return m_aLucene.readLockedAtomic (cb).booleanValue ();
  }

  @Nonnull
  public ESuccess createOrUpdateEntry (@Nonnull final IParticipantIdentifier aParticipantID,
                                       @Nonnull final PDExtendedBusinessCard aExtBI,
                                       @Nonnull final PDStoredMetaData aMetaData) throws IOException
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    ValueEnforcer.notNull (aExtBI, "ExtBI");
    ValueEnforcer.notNull (aMetaData, "MetaData");

    LOGGER.info ("Trying to create or update entry with participant ID '" +
                 aParticipantID.getURIEncoded () +
                 "' and " +
                 aExtBI.getBusinessCard ().businessEntities ().size () +
                 " entities");

    return m_aLucene.writeLockedAtomic ( () -> {
      final ICommonsList <Document> aDocs = new CommonsArrayList <> ();

      final PDBusinessCard aBI = aExtBI.getBusinessCard ();
      for (final PDBusinessEntity aBusinessEntity : aBI.businessEntities ())
      {
        // Convert entity to Lucene document
        final Document aDoc = new Document ();
        final StringBuilder aSBAllFields = new StringBuilder ();

        aDoc.add (PDField.PARTICIPANT_ID.getAsField (aParticipantID));
        aSBAllFields.append (PDField.PARTICIPANT_ID.getAsStorageValue (aParticipantID)).append (' ');
        if (aBusinessEntity.names ().size () == 1 && aBusinessEntity.names ().getFirstOrNull ().hasNoLanguageCode ())
        {
          // Single name without a language - legacy case
          final String sName = aBusinessEntity.names ().getFirstOrNull ().getName ();
          aDoc.add (PDField.NAME.getAsField (sName));
          aSBAllFields.append (sName).append (' ');
        }
        else
        {
          // More than one name or language
          for (final PDName aName : aBusinessEntity.names ())
          {
            final String sName = aName.getName ();
            aDoc.add (PDField.ML_NAME.getAsField (sName));
            aSBAllFields.append (sName).append (' ');

            final String sLanguage = StringHelper.getNotNull (aName.getLanguageCode ());
            aDoc.add (PDField.ML_LANGUAGE.getAsField (sLanguage));
            aSBAllFields.append (sLanguage).append (' ');
          }
        }
        if (aBusinessEntity.hasCountryCode ())
        {
          // Index all country codes in upper case (since 2017-09-20)
          final String sCountryCode = aBusinessEntity.getCountryCode ().toUpperCase (Locale.US);
          aDoc.add (PDField.COUNTRY_CODE.getAsField (sCountryCode));
          aSBAllFields.append (sCountryCode).append (' ');
        }
        // Add all document types to all documents
        for (final IDocumentTypeIdentifier aDocTypeID : aExtBI.getAllDocumentTypeIDs ())
        {
          aDoc.add (PDField.DOCTYPE_ID.getAsField (aDocTypeID));
          aSBAllFields.append (PDField.DOCTYPE_ID.getAsStorageValue (aDocTypeID)).append (' ');
        }
        if (aBusinessEntity.hasGeoInfo ())
        {
          aDoc.add (PDField.GEO_INFO.getAsField (aBusinessEntity.getGeoInfo ()));
          aSBAllFields.append (aBusinessEntity.getGeoInfo ()).append (' ');
        }
        for (final PDIdentifier aIdentifier : aBusinessEntity.identifiers ())
        {
          aDoc.add (PDField.IDENTIFIER_SCHEME.getAsField (aIdentifier.getScheme ()));
          aSBAllFields.append (aIdentifier.getScheme ()).append (' ');

          aDoc.add (PDField.IDENTIFIER_VALUE.getAsField (aIdentifier.getValue ()));
          aSBAllFields.append (aIdentifier.getValue ()).append (' ');
        }
        for (final String sWebSite : aBusinessEntity.websiteURIs ())
        {
          aDoc.add (PDField.WEBSITE_URI.getAsField (sWebSite));
          aSBAllFields.append (sWebSite).append (' ');
        }
        for (final PDContact aContact : aBusinessEntity.contacts ())
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
        if (aBusinessEntity.hasAdditionalInfo ())
        {
          aDoc.add (PDField.ADDITIONAL_INFO.getAsField (aBusinessEntity.getAdditionalInfo ()));
          aSBAllFields.append (aBusinessEntity.getAdditionalInfo ()).append (' ');
        }
        if (aBusinessEntity.hasRegistrationDate ())
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
        CollectionFind.getLastElement (aDocs).add (new Field (FIELD_GROUP_END, VALUE_GROUP_END, TYPE_GROUP_END));
      }
      // Delete all existing documents of the participant ID
      // and add the new ones to the index
      m_aLucene.updateDocuments (PDField.PARTICIPANT_ID.getExactMatchTerm (aParticipantID), aDocs);

      LOGGER.info ("Added " + aDocs.size () + " Lucene documents");
      AuditHelper.onAuditExecuteSuccess ("pd-indexer-create",
                                         aParticipantID.getURIEncoded (),
                                         Integer.valueOf (aDocs.size ()),
                                         aMetaData);
    });
  }

  @CheckForSigned
  public int deleteEntry (@Nonnull final IParticipantIdentifier aParticipantID,
                          @Nullable final PDStoredMetaData aMetaData,
                          final boolean bVerifyOwner) throws IOException
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");

    LOGGER.info ("Trying to delete entry with participant ID '" +
                 aParticipantID.getURIEncoded () +
                 "'" +
                 (bVerifyOwner && aMetaData != null ? " with owner ID '" + aMetaData.getOwnerID () + "'" : ""));

    Query aParticipantQuery = new TermQuery (PDField.PARTICIPANT_ID.getExactMatchTerm (aParticipantID));
    if (getCount (aParticipantQuery) == 0)
    {
      // Hack e.g. for 9925:everbinding
      final String sOrigValue = aParticipantID.getValue ();
      final String sUpperCaseValue = sOrigValue.toUpperCase (Locale.ROOT);
      if (!sUpperCaseValue.equals (sOrigValue))
      {
        // Something changed - try again
        // Force case sensitivity
        final IParticipantIdentifier aNewPID = new SimpleParticipantIdentifier (aParticipantID.getScheme (),
                                                                                sUpperCaseValue);
        final Query aOtherQuery = new TermQuery (PDField.PARTICIPANT_ID.getExactMatchTerm (aNewPID));
        if (getCount (aOtherQuery) > 0)
        {
          LOGGER.info ("Found something with '" + sUpperCaseValue + "' instead of '" + sOrigValue + "'");
          aParticipantQuery = aOtherQuery;
        }
      }
    }
    final Query aDeleteQuery;
    if (bVerifyOwner && aMetaData != null)
    {
      // Special handling for predefined owners
      final BooleanQuery.Builder aBuilderOr = new BooleanQuery.Builder ();

      // TODO the equals-check on deletion is to strict for Peppol
      aBuilderOr.add (new TermQuery (PDField.METADATA_OWNERID.getExactMatchTerm (aMetaData.getOwnerID ())),
                      Occur.SHOULD);
      aBuilderOr.add (new TermQuery (PDField.METADATA_OWNERID.getExactMatchTerm (CPDStorage.OWNER_DUPLICATE_ELIMINATION)),
                      Occur.SHOULD);
      aBuilderOr.add (new TermQuery (PDField.METADATA_OWNERID.getExactMatchTerm (CPDStorage.OWNER_IMPORT_TRIGGERED)),
                      Occur.SHOULD);
      aBuilderOr.add (new TermQuery (PDField.METADATA_OWNERID.getExactMatchTerm (CPDStorage.OWNER_MANUALLY_TRIGGERED)),
                      Occur.SHOULD);
      aBuilderOr.add (new TermQuery (PDField.METADATA_OWNERID.getExactMatchTerm (CPDStorage.OWNER_SYNC_JOB)),
                      Occur.SHOULD);

      aDeleteQuery = new BooleanQuery.Builder ().add (aParticipantQuery, Occur.MUST)
                                                .add (aBuilderOr.build (), Occur.MUST)
                                                .build ();
    }
    else
      aDeleteQuery = aParticipantQuery;
    final int nCount = getCount (aDeleteQuery);
    if (m_aLucene.writeLockedAtomic ( () -> {
      // Delete
      m_aLucene.deleteDocuments (aDeleteQuery);
    }).isFailure ())
    {
      LOGGER.error ("Failed to delete docs from the index using the query '" + aDeleteQuery + "'");
      return -1;
    }
    LOGGER.info ("Deleted " + nCount + " docs from the index using the query '" + aDeleteQuery + "'");
    AuditHelper.onAuditExecuteSuccess ("pd-indexer-delete",
                                       aParticipantID.getURIEncoded (),
                                       Integer.valueOf (nCount),
                                       aMetaData,
                                       Boolean.toString (bVerifyOwner));
    return nCount;
  }

  /**
   * Search all documents matching the passed query and pass the result on to the provided
   * {@link Consumer}.
   *
   * @param aQuery
   *        Query to execute. May not be <code>null</code>-
   * @param aCollector
   *        The Lucene collector to be used. May not be <code>null</code>.
   * @throws IOException
   *         On Lucene error
   * @see #getAllDocuments(Query,int)
   */
  public void searchAtomic (@Nonnull final Query aQuery, @Nonnull final Collector aCollector) throws IOException
  {
    ValueEnforcer.notNull (aQuery, "Query");
    ValueEnforcer.notNull (aCollector, "Collector");

    m_aLucene.readLockedAtomic ( () -> {
      final IndexSearcher aSearcher = m_aLucene.getSearcher ();
      if (aSearcher != null)
      {
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Searching Lucene: " + aQuery);

        // Search all documents, collect them
        _timedSearch ( () -> aSearcher.search (aQuery, aCollector), aQuery);
      }
      else
        LOGGER.error ("Failed to obtain IndexSearcher for " + aQuery);
      // Return values does not matter
      return null;
    });
  }

  @CheckForSigned
  public int getCount (@Nonnull final Query aQuery)
  {
    ValueEnforcer.notNull (aQuery, "Query");
    try
    {
      final TotalHitCountCollector aCollector = new TotalHitCountCollector ();
      searchAtomic (aQuery, aCollector);
      return aCollector.getTotalHits ();
    }
    catch (final IOException ex)
    {
      LOGGER.error ("Error counting documents with query " + aQuery, ex);
      return -1;
    }
  }

  /**
   * Search all documents matching the passed query and pass the result on to the provided
   * {@link Consumer}.
   *
   * @param aQuery
   *        Query to execute. May not be <code>null</code>.
   * @param nMaxResultCount
   *        Maximum number of results. Values &le; 0 mean all.
   * @param aFromDocumentConverter
   *        The function to extract data from the Lucene Document. May not be <code>null</code>.
   * @param aConsumer
   *        The consumer of the mapped objects. May not be <code>null</code>.
   * @throws IOException
   *         On Lucene error
   * @see #searchAtomic(Query, Collector)
   * @see #getAllDocuments(Query,int)
   */
  public <T> void searchAll (@Nonnull final Query aQuery,
                             @CheckForSigned final int nMaxResultCount,
                             @Nonnull final Function <Document, T> aFromDocumentConverter,
                             @Nonnull final Consumer <? super T> aConsumer) throws IOException
  {
    ValueEnforcer.notNull (aQuery, "Query");
    ValueEnforcer.notNull (aFromDocumentConverter, "FromDocumentConverter");
    ValueEnforcer.notNull (aConsumer, "Consumer");

    searchAll (aQuery, nMaxResultCount, aDoc -> aConsumer.accept (aFromDocumentConverter.apply (aDoc)));
  }

  public void searchAll (@Nonnull final Query aQuery,
                         @CheckForSigned final int nMaxResultCount,
                         @Nonnull final Consumer <Document> aConsumer) throws IOException
  {
    ValueEnforcer.notNull (aQuery, "Query");
    ValueEnforcer.notNull (aConsumer, "Consumer");
    if (nMaxResultCount <= 0)
    {
      // Search all
      final ObjIntConsumer <Document> aConverter = (aDoc, nDocID) -> aConsumer.accept (aDoc);
      final Collector aCollector = new AllDocumentsCollector (m_aLucene, aConverter);
      searchAtomic (aQuery, aCollector);
    }
    else
    {
      // Search top docs only
      // Lucene 8
      final TopScoreDocCollector aCollector = TopScoreDocCollector.create (nMaxResultCount, Integer.MAX_VALUE);
      // Lucene 7
      // final TopScoreDocCollector aCollector = TopScoreDocCollector.create
      // (nMaxResultCount);
      searchAtomic (aQuery, aCollector);
      for (final ScoreDoc aScoreDoc : aCollector.topDocs ().scoreDocs)
      {
        final Document aDoc = m_aLucene.getDocument (aScoreDoc.doc);
        if (aDoc == null)
          throw new IllegalStateException ("Failed to resolve Lucene Document with ID " + aScoreDoc.doc);
        // Pass to Consumer
        aConsumer.accept (aDoc);
      }
    }
  }

  /**
   * Search all documents matching the passed query and pass the result on to the provided
   * {@link Consumer}. This is a specific version of #searchAll(Query, int, Function, Consumer) with
   * {@link PDStoredBusinessEntity} objects.
   *
   * @param aQuery
   *        Query to execute. May not be <code>null</code>.
   * @param nMaxResultCount
   *        Maximum number of results. Values &le; 0 mean all.
   * @param aConsumer
   *        The consumer of the {@link PDStoredBusinessEntity} objects. May not be
   *        <code>null</code>.
   * @throws IOException
   *         On Lucene error
   * @see #searchAtomic(Query, Collector)
   * @see #getAllDocuments(Query,int)
   */
  public void searchAllDocuments (@Nonnull final Query aQuery,
                                  @CheckForSigned final int nMaxResultCount,
                                  @Nonnull final Consumer <? super PDStoredBusinessEntity> aConsumer) throws IOException
  {
    searchAll (aQuery, nMaxResultCount, PDStoredBusinessEntity::create, aConsumer);
  }

  /**
   * Get all {@link PDStoredBusinessEntity} objects matching the provided query. This is a
   * specialization of {@link #searchAllDocuments(Query, int, Consumer)}.
   *
   * @param aQuery
   *        The query to be executed. May not be <code>null</code>.
   * @param nMaxResultCount
   *        Maximum number of results. Values &le; 0 mean all.
   * @return A non-<code>null</code> but maybe empty list of matching documents
   * @see #searchAllDocuments(Query, int, Consumer)
   */
  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <PDStoredBusinessEntity> getAllDocuments (@Nonnull final Query aQuery,
                                                                @CheckForSigned final int nMaxResultCount)
  {
    final ICommonsList <PDStoredBusinessEntity> aTargetList = new CommonsArrayList <> ();
    try
    {
      searchAllDocuments (aQuery, nMaxResultCount, aTargetList::add);
    }
    catch (final IOException ex)
    {
      LOGGER.error ("Error searching for documents with query " + aQuery, ex);
    }
    return aTargetList;
  }

  @Nonnull
  public ICommonsList <PDStoredBusinessEntity> getAllDocumentsOfParticipant (@Nonnull final IParticipantIdentifier aParticipantID)
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    ICommonsList <PDStoredBusinessEntity> ret = getAllDocuments (new TermQuery (PDField.PARTICIPANT_ID.getExactMatchTerm (aParticipantID)),
                                                                 -1);
    if (ret.isEmpty ())
    {
      // Hack e.g. for 9925:everbinding
      final String sOrigValue = aParticipantID.getValue ();
      final String sUpperCaseValue = sOrigValue.toUpperCase (Locale.ROOT);
      if (!sUpperCaseValue.equals (sOrigValue))
      {
        // Something changed - try again
        // Force case sensitivity
        final IParticipantIdentifier aNewPID = new SimpleParticipantIdentifier (aParticipantID.getScheme (),
                                                                                sUpperCaseValue);
        ret = getAllDocuments (new TermQuery (PDField.PARTICIPANT_ID.getExactMatchTerm (aNewPID)), -1);
        if (ret.isNotEmpty ())
        {
          LOGGER.info ("Found something with '" + sUpperCaseValue + "' instead of '" + sOrigValue + "'");
        }
      }
    }
    return ret;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsSortedMap <IParticipantIdentifier, MutableInt> getAllContainedParticipantIDs ()
  {
    // Map from ID to entity count
    final ICommonsSortedMap <IParticipantIdentifier, MutableInt> aTargetSet = new CommonsTreeMap <> ();
    final Query aQuery = new MatchAllDocsQuery ();
    try
    {
      final ObjIntConsumer <Document> aConsumer = (aDoc, nDocID) -> {
        final IParticipantIdentifier aResolvedParticipantID = PDField.PARTICIPANT_ID.getDocValue (aDoc);
        aTargetSet.computeIfAbsent (aResolvedParticipantID, k -> new MutableInt (0)).inc ();
      };
      final Collector aCollector = new AllDocumentsCollector (m_aLucene, aConsumer);
      searchAtomic (aQuery, aCollector);
    }
    catch (final IOException ex)
    {
      LOGGER.error ("Error searching for documents with query " + aQuery, ex);
    }
    return aTargetSet;
  }

  @CheckForSigned
  public int getContainedParticipantCount ()
  {
    return getCount (new MatchAllDocsQuery ());
  }

  /**
   * Group the passed document list by participant ID
   *
   * @param aDocs
   *        The document list to group.
   * @return A non-<code>null</code> ordered map with the results. Order is like the order of the
   *         input list.
   */
  @Nonnull
  @ReturnsMutableCopy
  public static ICommonsMap <IParticipantIdentifier, ICommonsList <PDStoredBusinessEntity>> getGroupedByParticipantID (@Nonnull final Iterable <PDStoredBusinessEntity> aDocs)
  {
    final ICommonsMap <IParticipantIdentifier, ICommonsList <PDStoredBusinessEntity>> ret = new CommonsLinkedHashMap <> ();
    for (final PDStoredBusinessEntity aDoc : aDocs)
      ret.computeIfAbsent (aDoc.getParticipantID (), k -> new CommonsArrayList <> ()).add (aDoc);
    return ret;
  }
}
