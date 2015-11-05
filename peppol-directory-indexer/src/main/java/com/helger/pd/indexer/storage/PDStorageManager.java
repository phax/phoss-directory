/**
 * Copyright (C) 2015 Philip Helger (www.helger.com)
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

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.callback.IThrowingCallable;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.multimap.IMultiMapListBased;
import com.helger.commons.collection.multimap.MultiLinkedHashMapArrayListBased;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.StringHelper;
import com.helger.pd.businessinformation.PDBusinessContactType;
import com.helger.pd.businessinformation.PDBusinessInformationType;
import com.helger.pd.businessinformation.PDEntityType;
import com.helger.pd.businessinformation.PDExtendedBusinessInformation;
import com.helger.pd.businessinformation.PDIdentifierType;
import com.helger.pd.indexer.lucene.AllDocumentsCollector;
import com.helger.pd.indexer.lucene.PDLucene;
import com.helger.peppol.identifier.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.IdentifierHelper;
import com.helger.peppol.identifier.participant.IPeppolParticipantIdentifier;
import com.helger.photon.basic.security.audit.AuditHelper;
import com.helger.web.datetime.PDTWebDateHelper;

/**
 * The global storage manager that wraps the used Lucene index.
 *
 * @author Philip Helger
 */
@Immutable
public final class PDStorageManager implements Closeable
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (PDStorageManager.class);
  private static final IntField FIELD_VALUE_DELETED = new IntField (CPDStorage.FIELD_DELETED, 1, Store.NO);
  private static final FieldType TYPE_GROUP_END = new FieldType ();
  private static final String VALUE_GROUP_END = "x";

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

  @Nonnull
  private static Term _createParticipantTerm (@Nonnull final IPeppolParticipantIdentifier aParticipantID)
  {
    return new Term (CPDStorage.FIELD_PARTICIPANTID, aParticipantID.getURIEncoded ());
  }

  public boolean containsEntry (@Nullable final IPeppolParticipantIdentifier aParticipantID) throws IOException
  {
    if (aParticipantID == null)
      return false;

    // Must be "Exception" because of JDK commandline compiler issue
    final IThrowingCallable <Boolean, Exception> cb = () -> {
      final IndexSearcher aSearcher = m_aLucene.getSearcher ();
      if (aSearcher != null)
      {
        // Search only documents that do not have the deleted field
        final Query aQuery = new TermQuery (_createParticipantTerm (aParticipantID));
        final TopDocs aTopDocs = aSearcher.search (PDQueryManager.andNotDeleted (aQuery), 1);
        if (aTopDocs.totalHits > 0)
          return Boolean.TRUE;
      }
      return Boolean.FALSE;
    };
    return m_aLucene.callAtomic (cb).booleanValue ();
  }

  @Nonnull
  public ESuccess deleteEntry (@Nonnull final IPeppolParticipantIdentifier aParticipantID, @Nonnull final PDDocumentMetaData aMetaData) throws IOException
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    ValueEnforcer.notNull (aMetaData, "MetaData");

    return m_aLucene.runAtomic ( () -> {
      final List <Document> aDocuments = new ArrayList <> ();

      // Get all documents to be marked as deleted
      final IndexSearcher aSearcher = m_aLucene.getSearcher ();
      if (aSearcher != null)
        aSearcher.search (new TermQuery (_createParticipantTerm (aParticipantID)), new AllDocumentsCollector (m_aLucene, aDoc -> aDocuments.add (aDoc)));

      if (!aDocuments.isEmpty ())
      {
        // Mark document as deleted
        for (final Document aDocument : aDocuments)
          aDocument.add (FIELD_VALUE_DELETED);

        // Update the documents
        m_aLucene.updateDocuments (_createParticipantTerm (aParticipantID), aDocuments);
      }

      s_aLogger.info ("Marked " + aDocuments.size () + " Lucene documents as deleted");
      AuditHelper.onAuditExecuteSuccess ("pyp-indexer-delete", aParticipantID.getURIEncoded (), Integer.valueOf (aDocuments.size ()), aMetaData);
    });
  }

  @Nonnull
  public ESuccess createOrUpdateEntry (@Nonnull final IPeppolParticipantIdentifier aParticipantID,
                                       @Nonnull final PDExtendedBusinessInformation aExtBI,
                                       @Nonnull final PDDocumentMetaData aMetaData) throws IOException
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    ValueEnforcer.notNull (aExtBI, "ExtBI");
    ValueEnforcer.notNull (aMetaData, "MetaData");

    return m_aLucene.runAtomic ( () -> {
      final List <Document> aDocs = new ArrayList <> ();

      final PDBusinessInformationType aBI = aExtBI.getBusinessInformation ();
      for (final PDEntityType aEntity : aBI.getEntity ())
      {
        // Convert entity to Lucene document
        final Document aDoc = new Document ();
        final StringBuilder aSBAllFields = new StringBuilder ();

        aDoc.add (new StringField (CPDStorage.FIELD_PARTICIPANTID, aParticipantID.getURIEncoded (), Store.YES));
        aSBAllFields.append (aParticipantID.getURIEncoded ()).append (' ');

        // Add all document types to all documents
        for (final IDocumentTypeIdentifier aDocTypeID : aExtBI.getAllDocumentTypeIDs ())
        {
          final String sDocTypeID = IdentifierHelper.getIdentifierURIEncoded (aDocTypeID);
          aDoc.add (new StringField (CPDStorage.FIELD_DOCUMENT_TYPE_ID, sDocTypeID, Store.YES));
          aSBAllFields.append (sDocTypeID).append (' ');
        }

        if (aEntity.getCountryCode () != null)
        {
          aDoc.add (new StringField (CPDStorage.FIELD_COUNTRY_CODE, aEntity.getCountryCode (), Store.YES));
          aSBAllFields.append (aEntity.getCountryCode ()).append (' ');
        }

        if (aEntity.getRegistrationDate () != null)
        {
          final String sDate = PDTWebDateHelper.getAsStringXSD (aEntity.getRegistrationDate ());
          aDoc.add (new StringField (CPDStorage.FIELD_REGISTRATION_DATE, sDate, Store.YES));
          aSBAllFields.append (sDate).append (' ');
        }

        if (aEntity.getName () != null)
        {
          aDoc.add (new TextField (CPDStorage.FIELD_NAME, aEntity.getName (), Store.YES));
          aSBAllFields.append (aEntity.getName ()).append (' ');
        }

        if (aEntity.getGeoInfo () != null)
        {
          aDoc.add (new TextField (CPDStorage.FIELD_GEOINFO, aEntity.getGeoInfo (), Store.YES));
          aSBAllFields.append (aEntity.getGeoInfo ()).append (' ');
        }

        for (final PDIdentifierType aIdentifier : aEntity.getIdentifier ())
        {
          aDoc.add (new TextField (CPDStorage.FIELD_IDENTIFIER_TYPE, aIdentifier.getType (), Store.YES));
          aSBAllFields.append (aIdentifier.getType ()).append (' ');

          aDoc.add (new TextField (CPDStorage.FIELD_IDENTIFIER, aIdentifier.getValue (), Store.YES));
          aSBAllFields.append (aIdentifier.getValue ()).append (' ');
        }

        for (final String sWebSite : aEntity.getWebSite ())
        {
          aDoc.add (new TextField (CPDStorage.FIELD_WEBSITE, sWebSite, Store.YES));
          aSBAllFields.append (sWebSite).append (' ');
        }

        for (final PDBusinessContactType aBusinessContact : aEntity.getBusinessContact ())
        {
          final String sDescription = StringHelper.getNotNull (aBusinessContact.getDescription ());
          aDoc.add (new TextField (CPDStorage.FIELD_BUSINESS_CONTACT_DESCRIPTION, sDescription, Store.YES));
          aSBAllFields.append (sDescription).append (' ');

          final String sName = StringHelper.getNotNull (aBusinessContact.getName ());
          aDoc.add (new TextField (CPDStorage.FIELD_BUSINESS_CONTACT_NAME, sName, Store.YES));
          aSBAllFields.append (sName).append (' ');

          final String sPhone = StringHelper.getNotNull (aBusinessContact.getPhoneNumber ());
          aDoc.add (new TextField (CPDStorage.FIELD_BUSINESS_CONTACT_PHONE, sPhone, Store.YES));
          aSBAllFields.append (sPhone).append (' ');

          final String sEmail = StringHelper.getNotNull (aBusinessContact.getEmail ());
          aDoc.add (new TextField (CPDStorage.FIELD_BUSINESS_CONTACT_EMAIL, sEmail, Store.YES));
          aSBAllFields.append (sEmail).append (' ');
        }

        if (aEntity.getFreeText () != null)
        {
          aDoc.add (new TextField (CPDStorage.FIELD_FREETEXT, aEntity.getFreeText (), Store.YES));
          aSBAllFields.append (aEntity.getFreeText ()).append (' ');
        }

        // Add the "all" field - no need to store
        aDoc.add (new TextField (CPDStorage.FIELD_ALL_FIELDS, aSBAllFields.toString (), Store.NO));

        // Add meta data (not part of the "all field" field!)
        aDoc.add (new LongField (CPDStorage.FIELD_METADATA_CREATIONDT, aMetaData.getCreationDTMillis (), Store.YES));
        aDoc.add (new StringField (CPDStorage.FIELD_METADATA_OWNERID, aMetaData.getOwnerID (), Store.YES));
        aDoc.add (new StringField (CPDStorage.FIELD_METADATA_REQUESTING_HOST, aMetaData.getRequestingHost (), Store.YES));

        aDocs.add (aDoc);
      }

      if (!aDocs.isEmpty ())
      {
        // Add "group end" marker
        CollectionHelper.getLastElement (aDocs).add (new Field (CPDStorage.FIELD_GROUP_END, VALUE_GROUP_END, TYPE_GROUP_END));
      }

      // Delete all existing documents of the participant ID
      // and add the new ones to the index
      m_aLucene.updateDocuments (_createParticipantTerm (aParticipantID), aDocs);

      s_aLogger.info ("Added " + aDocs.size () + " Lucene documents");
      AuditHelper.onAuditExecuteSuccess ("pyp-indexer-create", aParticipantID.getURIEncoded (), Integer.valueOf (aDocs.size ()), aMetaData);
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

        // Search all documents, convert them to StoredDocument and pass them to
        // the provided consumer
        aSearcher.search (aQuery, aCollector);
      }
      else
        s_aLogger.warn ("Failed to obtain IndexSearcher");
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
  public void searchAllDocuments (@Nonnull final Query aQuery, @Nonnull final Consumer <PDStoredDocument> aConsumer) throws IOException
  {
    ValueEnforcer.notNull (aQuery, "Query");
    ValueEnforcer.notNull (aConsumer, "Consumer");

    searchAtomic (aQuery, new AllDocumentsCollector (m_aLucene, aDoc -> aConsumer.accept (PDStoredDocument.create (aDoc))));
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
  public List <PDStoredDocument> getAllDocuments (@Nonnull final Query aQuery)
  {
    final List <PDStoredDocument> aTargetList = new ArrayList <> ();
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
  public List <PDStoredDocument> getAllDocumentsOfParticipant (@Nonnull final IPeppolParticipantIdentifier aParticipantID)
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    return getAllDocuments (new TermQuery (_createParticipantTerm (aParticipantID)));
  }

  /**
   * Get all documents matching the passed country code
   *
   * @param sCountryCode
   *        Country code to search. May not be <code>null</code>.
   * @return Non-<code>null</code> but maybe empty list of documents
   */
  @Nonnull
  public List <PDStoredDocument> getAllDocumentsOfCountryCode (@Nonnull final String sCountryCode)
  {
    ValueEnforcer.notNull (sCountryCode, "CountryCode");
    return getAllDocuments (new TermQuery (new Term (CPDStorage.FIELD_COUNTRY_CODE, sCountryCode)));
  }

  @Nonnull
  @ReturnsMutableCopy
  public Set <String> getAllContainedParticipantIDs ()
  {
    final Set <String> aTargetList = new TreeSet <> ();
    final Query aQuery = PDQueryManager.andNotDeleted (new WildcardQuery (new Term (CPDStorage.FIELD_ALL_FIELDS, "*")));
    try
    {
      searchAtomic (aQuery, new AllDocumentsCollector (m_aLucene, aDoc -> aTargetList.add (aDoc.get (CPDStorage.FIELD_PARTICIPANTID))));
    }
    catch (final IOException ex)
    {
      s_aLogger.error ("Error searching for documents with query " + aQuery, ex);
    }
    return aTargetList;
  }

  /**
   * Group the passed document list by participant ID
   *
   * @param aDocs
   *        The document list to group.
   * @return A non-<code>null</code> LinkedHashMap with the results. Order is
   *         like the order of the input list.
   */
  @Nonnull
  @ReturnsMutableCopy
  public static IMultiMapListBased <String, PDStoredDocument> getGroupedByParticipantID (@Nonnull final List <PDStoredDocument> aDocs)
  {
    final MultiLinkedHashMapArrayListBased <String, PDStoredDocument> ret = new MultiLinkedHashMapArrayListBased <> ();
    for (final PDStoredDocument aDoc : aDocs)
      ret.putSingle (aDoc.getParticipantID (), aDoc);
    return ret;
  }
}
