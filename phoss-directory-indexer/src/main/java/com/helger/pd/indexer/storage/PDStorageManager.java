/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.CheckForSigned;
import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.CGlobal;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.functional.IThrowingSupplier;
import com.helger.base.numeric.mutable.MutableInt;
import com.helger.base.state.ESuccess;
import com.helger.base.string.StringHelper;
import com.helger.base.timing.StopWatch;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsLinkedHashMap;
import com.helger.collection.commons.CommonsTreeMap;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsMap;
import com.helger.collection.commons.ICommonsSortedMap;
import com.helger.datetime.web.PDTWebDateHelper;
import com.helger.pd.indexer.businesscard.PDExtendedBusinessCard;
import com.helger.pd.indexer.mgr.IPDStorageManager;
import com.helger.pd.indexer.searchindex.EPDIndexFieldStore;
import com.helger.pd.indexer.searchindex.EPDIndexFieldTokenize;
import com.helger.pd.indexer.searchindex.IPDIndex;
import com.helger.pd.indexer.searchindex.PDIndexDocument;
import com.helger.pd.indexer.searchindex.PDIndexField;
import com.helger.pd.indexer.searchindex.query.EPDIndexQueryOccur;
import com.helger.pd.indexer.searchindex.query.IPDIndexQuery;
import com.helger.pd.indexer.searchindex.query.PDIndexQueryBool;
import com.helger.pd.indexer.searchindex.query.PDIndexQueryMatchAll;
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

import jakarta.annotation.Nullable;

/**
 * The global storage manager that wraps the used search index.
 *
 * @author Philip Helger
 */
@Immutable
public final class PDStorageManager implements IPDStorageManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PDStorageManager.class);
  private static final IMutableStatisticsHandlerKeyedTimer STATS_QUERY_TIMER = StatisticsManager.getKeyedTimerHandler (PDStorageManager.class.getName () +
                                                                                                                       "$query");

  private final IPDIndex m_aIndex;

  public PDStorageManager (@NonNull final IPDIndex aIndex)
  {
    m_aIndex = ValueEnforcer.notNull (aIndex, "Index");
  }

  public void close () throws IOException
  {
    m_aIndex.close ();
  }

  private static <T> T _timedSearch (@NonNull final IThrowingSupplier <T, IOException> aRunnable,
                                     @NonNull final IPDIndexQuery aQuery) throws IOException
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

      // 1 seconds bloats the log - use 2 seconds
      if (nMillis > 2 * CGlobal.MILLISECONDS_PER_SECOND)
        LOGGER.warn ("Index Query " + aQuery + " took too long: " + nMillis + "ms");
    }
  }

  public boolean containsEntry (@Nullable final IParticipantIdentifier aParticipantID) throws IOException
  {
    if (aParticipantID == null)
      return false;

    // Search only documents that do not have the deleted field
    final IPDIndexQuery aQuery = PDField.PARTICIPANT_ID.getExactMatchQuery (aParticipantID);
    final int nCount = _timedSearch (() -> Integer.valueOf (m_aIndex.getCount (aQuery)), aQuery).intValue ();
    return nCount > 0;
  }

  @NonNull
  public ESuccess createOrUpdateEntry (@NonNull final IParticipantIdentifier aParticipantID,
                                       @NonNull final PDExtendedBusinessCard aExtBI,
                                       @NonNull final PDStoredMetaData aMetaData) throws IOException
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    ValueEnforcer.notNull (aExtBI, "ExtBI");
    ValueEnforcer.notNull (aMetaData, "MetaData");

    LOGGER.info ("Trying to create or update entry with participant ID '" +
                 aParticipantID.getURIEncoded () +
                 "' and " +
                 aExtBI.getBusinessCard ().businessEntities ().size () +
                 " entities");

    try
    {
      final ICommonsList <PDIndexDocument> aDocs = new CommonsArrayList <> ();

      final PDBusinessCard aBI = aExtBI.getBusinessCard ();
      for (final PDBusinessEntity aBusinessEntity : aBI.businessEntities ())
      {
        // Convert entity to index document
        final PDIndexDocument aDoc = new PDIndexDocument ();
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
        aDoc.add (PDIndexField.createString (CPDStorage.FIELD_ALL_FIELDS,
                                             aSBAllFields.toString (),
                                             EPDIndexFieldStore.NO,
                                             EPDIndexFieldTokenize.TOKENIZE));

        // Add meta data (not part of the "all field" field!)
        aDoc.add (PDField.METADATA_CREATIONDT.getAsField (aMetaData.getCreationDT ()));
        aDoc.add (PDField.METADATA_OWNERID.getAsField (aMetaData.getOwnerID ()));
        aDoc.add (PDField.METADATA_REQUESTING_HOST.getAsField (aMetaData.getRequestingHost ()));

        aDocs.add (aDoc);
      }
      // Delete all existing documents of the participant ID
      // and add the new ones to the index
      m_aIndex.updateDocuments (PDField.PARTICIPANT_ID.getExactMatchQuery (aParticipantID), aDocs);

      LOGGER.info ("Added " + aDocs.size () + " index documents");
      AuditHelper.onAuditExecuteSuccess ("pd-indexer-create",
                                         aParticipantID.getURIEncoded (),
                                         Integer.valueOf (aDocs.size ()),
                                         aMetaData);
      return ESuccess.SUCCESS;
    }
    catch (final IllegalStateException ex)
    {
      // When index is closing
      return ESuccess.FAILURE;
    }
  }

  @CheckForSigned
  public int deleteEntry (@NonNull final IParticipantIdentifier aParticipantID,
                          @Nullable final PDStoredMetaData aMetaData,
                          final boolean bVerifyOwner) throws IOException
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");

    LOGGER.info ("Trying to delete entry with participant ID '" +
                 aParticipantID.getURIEncoded () +
                 "'" +
                 (bVerifyOwner && aMetaData != null ? " with owner ID '" + aMetaData.getOwnerID () + "'" : ""));

    IPDIndexQuery aParticipantQuery = PDField.PARTICIPANT_ID.getExactMatchQuery (aParticipantID);
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
        final IPDIndexQuery aOtherQuery = PDField.PARTICIPANT_ID.getExactMatchQuery (aNewPID);
        if (getCount (aOtherQuery) > 0)
        {
          LOGGER.info ("Found something with '" + sUpperCaseValue + "' instead of '" + sOrigValue + "'");
          aParticipantQuery = aOtherQuery;
        }
      }
    }

    final IPDIndexQuery aDeleteQuery;
    if (bVerifyOwner && aMetaData != null)
    {
      // Special handling for predefined owners
      final PDIndexQueryBool.Builder aBuilderOr = new PDIndexQueryBool.Builder ();

      if (false)
      {
        // TODO the equals-check on deletion is to strict for Peppol
        // If the below Prefix Query works, this check should be ignored
        aBuilderOr.add (PDField.METADATA_OWNERID.getExactMatchQuery (aMetaData.getOwnerID ()),
                        EPDIndexQueryOccur.SHOULD);
      }
      // Since 2025-11-03 use a prefix query instead of an exact match query, because the stored
      // OwnerID is longer (incl. serial number) then the provided OwnerID (without serial number)
      aBuilderOr.add (PDField.METADATA_OWNERID.getPrefixQuery (aMetaData.getOwnerID ()), EPDIndexQueryOccur.SHOULD);
      aBuilderOr.add (PDField.METADATA_OWNERID.getExactMatchQuery (CPDStorage.OWNER_DUPLICATE_ELIMINATION),
                      EPDIndexQueryOccur.SHOULD);
      aBuilderOr.add (PDField.METADATA_OWNERID.getExactMatchQuery (CPDStorage.OWNER_IMPORT_TRIGGERED),
                      EPDIndexQueryOccur.SHOULD);
      aBuilderOr.add (PDField.METADATA_OWNERID.getExactMatchQuery (CPDStorage.OWNER_MANUALLY_TRIGGERED),
                      EPDIndexQueryOccur.SHOULD);
      aBuilderOr.add (PDField.METADATA_OWNERID.getExactMatchQuery (CPDStorage.OWNER_SYNC_JOB),
                      EPDIndexQueryOccur.SHOULD);

      aDeleteQuery = new PDIndexQueryBool.Builder ().add (aParticipantQuery, EPDIndexQueryOccur.MUST)
                                                    .add (aBuilderOr.build (), EPDIndexQueryOccur.MUST)
                                                    .build ();
    }
    else
      aDeleteQuery = aParticipantQuery;

    final int nCount = getCount (aDeleteQuery);
    try
    {
      // Delete
      m_aIndex.deleteDocuments (aDeleteQuery);
    }
    catch (final Exception ex)
    {
      // E.g. the index is closing
      LOGGER.error ("Failed to delete docs from the index using the query '" + aDeleteQuery + "'");
      AuditHelper.onAuditExecuteFailure ("pd-indexer-delete",
                                         aParticipantID.getURIEncoded (),
                                         Integer.valueOf (nCount),
                                         aMetaData,
                                         Boolean.toString (bVerifyOwner),
                                         ex.getMessage ());
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

  @CheckForSigned
  public int getCount (@NonNull final IPDIndexQuery aQuery)
  {
    ValueEnforcer.notNull (aQuery, "Query");
    try
    {
      return _timedSearch (() -> Integer.valueOf (m_aIndex.getCount (aQuery)), aQuery).intValue ();
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
   *        The function to extract data from the index document. May not be <code>null</code>.
   * @param aConsumer
   *        The consumer of the mapped objects. May not be <code>null</code>.
   * @return The total number of documents matching the query, independent of the provided maximum
   *         result count. Always &ge; 0.
   * @throws IOException
   *         On index error
   * @see #getAllDocuments(IPDIndexQuery,int)
   */
  @CheckForSigned
  public <T> int searchAll (@NonNull final IPDIndexQuery aQuery,
                            @CheckForSigned final int nMaxResultCount,
                            @NonNull final Function <PDIndexDocument, T> aFromDocumentConverter,
                            @NonNull final Consumer <? super T> aConsumer) throws IOException
  {
    ValueEnforcer.notNull (aQuery, "Query");
    ValueEnforcer.notNull (aFromDocumentConverter, "FromDocumentConverter");
    ValueEnforcer.notNull (aConsumer, "Consumer");

    return searchAll (aQuery, nMaxResultCount, aDoc -> aConsumer.accept (aFromDocumentConverter.apply (aDoc)));
  }

  @CheckForSigned
  public int searchAll (@NonNull final IPDIndexQuery aQuery,
                        @CheckForSigned final int nMaxResultCount,
                        @NonNull final Consumer <PDIndexDocument> aConsumer) throws IOException
  {
    ValueEnforcer.notNull (aQuery, "Query");
    ValueEnforcer.notNull (aConsumer, "Consumer");

    final Integer aTotalHitCount = _timedSearch (() -> Integer.valueOf (m_aIndex.searchAll (aQuery,
                                                                                            nMaxResultCount,
                                                                                            aConsumer)),
                                                 aQuery);
    return aTotalHitCount.intValue ();
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
   * @return The total number of business entities matching the query, independent of the provided
   *         maximum result count. Always &ge; 0.
   * @throws IOException
   *         On index error
   * @see #getAllDocuments(IPDIndexQuery,int)
   */
  @CheckForSigned
  public int searchAllDocuments (@NonNull final IPDIndexQuery aQuery,
                                 @CheckForSigned final int nMaxResultCount,
                                 @NonNull final Consumer <? super PDStoredBusinessEntity> aConsumer) throws IOException
  {
    return searchAll (aQuery, nMaxResultCount, PDStoredBusinessEntity::create, aConsumer);
  }

  /**
   * Get all {@link PDStoredBusinessEntity} objects matching the provided query, together with the
   * total number of matching business entities. The total hit count is a by-product of the search
   * itself, so this method requires a single index query only, compared to the combination of
   * {@link #getAllDocuments(IPDIndexQuery, int)} and {@link #getCount(IPDIndexQuery)}. Because both
   * numbers originate from the same query, they are always consistent with each other.
   *
   * @param aQuery
   *        The query to be executed. May not be <code>null</code>.
   * @param nMaxResultCount
   *        Maximum number of results. Values &le; 0 mean all.
   * @return Never <code>null</code>. In case of an error, the contained total hit count is -1 and
   *         the contained list only holds the business entities that were found before the error
   *         occurred.
   * @see #searchAllDocuments(IPDIndexQuery, int, Consumer)
   * @since 0.18.0
   */
  @NonNull
  public PDSearchResult search (@NonNull final IPDIndexQuery aQuery, @CheckForSigned final int nMaxResultCount)
  {
    final ICommonsList <PDStoredBusinessEntity> aTargetList = new CommonsArrayList <> ();
    int nTotalHitCount;
    try
    {
      nTotalHitCount = searchAllDocuments (aQuery, nMaxResultCount, aTargetList::add);
    }
    catch (final IOException ex)
    {
      LOGGER.error ("Error searching for documents with query " + aQuery, ex);
      nTotalHitCount = -1;
    }
    return new PDSearchResult (aTargetList, nTotalHitCount);
  }

  /**
   * Get all {@link PDStoredBusinessEntity} objects matching the provided query. This is a
   * specialization of {@link #searchAllDocuments(IPDIndexQuery, int, Consumer)}.
   *
   * @param aQuery
   *        The query to be executed. May not be <code>null</code>.
   * @param nMaxResultCount
   *        Maximum number of results. Values &le; 0 mean all.
   * @return A non-<code>null</code> but maybe empty list of matching documents
   * @see #searchAllDocuments(IPDIndexQuery, int, Consumer)
   * @see #search(IPDIndexQuery, int)
   */
  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <PDStoredBusinessEntity> getAllDocuments (@NonNull final IPDIndexQuery aQuery,
                                                                @CheckForSigned final int nMaxResultCount)
  {
    return search (aQuery, nMaxResultCount).allEntities ();
  }

  @NonNull
  public ICommonsList <PDStoredBusinessEntity> getAllDocumentsOfParticipant (@NonNull final IParticipantIdentifier aParticipantID)
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    ICommonsList <PDStoredBusinessEntity> ret = getAllDocuments (PDField.PARTICIPANT_ID.getExactMatchQuery (aParticipantID),
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
        ret = getAllDocuments (PDField.PARTICIPANT_ID.getExactMatchQuery (aNewPID), -1);
        if (ret.isNotEmpty ())
        {
          LOGGER.info ("Found something with '" + sUpperCaseValue + "' instead of '" + sOrigValue + "'");
        }
      }
    }
    return ret;
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsSortedMap <IParticipantIdentifier, MutableInt> getAllContainedParticipantIDs ()
  {
    // Map from ID to entity count
    final ICommonsSortedMap <IParticipantIdentifier, MutableInt> aTargetSet = new CommonsTreeMap <> ();
    final IPDIndexQuery aQuery = PDIndexQueryMatchAll.INSTANCE;
    try
    {
      searchAll (aQuery, -1, aDoc -> {
        final IParticipantIdentifier aResolvedParticipantID = PDField.PARTICIPANT_ID.getDocValue (aDoc);
        if (aResolvedParticipantID != null)
          aTargetSet.computeIfAbsent (aResolvedParticipantID, _ -> new MutableInt (0)).inc ();
      });
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
    return getCount (PDIndexQueryMatchAll.INSTANCE);
  }

  /**
   * Group the passed document list by participant ID
   *
   * @param aDocs
   *        The document list to group.
   * @return A non-<code>null</code> ordered map with the results. Order is like the order of the
   *         input list.
   */
  @NonNull
  @ReturnsMutableCopy
  public static ICommonsMap <IParticipantIdentifier, ICommonsList <PDStoredBusinessEntity>> getGroupedByParticipantID (@NonNull final Iterable <PDStoredBusinessEntity> aDocs)
  {
    final ICommonsMap <IParticipantIdentifier, ICommonsList <PDStoredBusinessEntity>> ret = new CommonsLinkedHashMap <> ();
    for (final PDStoredBusinessEntity aDoc : aDocs)
    {
      final IParticipantIdentifier aPID = aDoc.getParticipantID ();
      if (aPID != null)
        ret.computeIfAbsent (aPID, _ -> new CommonsArrayList <> ()).add (aDoc);
    }
    return ret;
  }
}
