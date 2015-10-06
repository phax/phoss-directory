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
package com.helger.pyp.storage;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.multimap.IMultiMapListBased;
import com.helger.commons.collection.multimap.MultiLinkedHashMapArrayListBased;
import com.helger.commons.state.ESuccess;
import com.helger.peppol.identifier.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.IdentifierHelper;
import com.helger.peppol.identifier.participant.IPeppolParticipantIdentifier;
import com.helger.photon.basic.security.audit.AuditHelper;
import com.helger.pyp.businessinformation.BusinessInformationType;
import com.helger.pyp.businessinformation.EntityType;
import com.helger.pyp.businessinformation.IdentifierType;
import com.helger.pyp.businessinformation.PYPExtendedBusinessInformation;
import com.helger.pyp.lucene.AllDocumentsCollector;
import com.helger.pyp.lucene.PYPLucene;

/**
 * The global storage manager that wraps the used Lucene index.
 *
 * @author Philip Helger
 */
@Immutable
public final class PYPStorageManager implements Closeable
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (PYPStorageManager.class);
  private static final IntField FIELD_VALUE_DELETED = new IntField (CPYPStorage.FIELD_DELETED, 1, Store.NO);
  private static final FieldType TYPE_GROUP_END = new FieldType ();
  private static final String VALUE_GROUP_END = "x";

  static
  {
    TYPE_GROUP_END.setStored (false);
    TYPE_GROUP_END.setIndexOptions (IndexOptions.DOCS);
    TYPE_GROUP_END.setOmitNorms (true);
    TYPE_GROUP_END.freeze ();
  }

  private final PYPLucene m_aLucene;

  public PYPStorageManager (@Nonnull final PYPLucene aLucene)
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
    return new Term (CPYPStorage.FIELD_PARTICIPANTID, aParticipantID.getURIEncoded ());
  }

  public boolean containsEntry (@Nullable final IPeppolParticipantIdentifier aParticipantID) throws IOException
  {
    if (aParticipantID == null)
      return false;

    return m_aLucene.runAtomic ( () -> {
      final IndexSearcher aSearcher = m_aLucene.getSearcher ();
      if (aSearcher != null)
      {
        // Search only documents that do not have the deleted field
        final Query aQuery = new TermQuery (new Term (CPYPStorage.FIELD_PARTICIPANTID,
                                                      aParticipantID.getURIEncoded ()));
        final TopDocs aTopDocs = aSearcher.search (PYPQueryManager.andNotDeleted (aQuery), 1);
        if (aTopDocs.totalHits > 0)
          return Boolean.TRUE;
      }
      return Boolean.FALSE;
    }).booleanValue ();
  }

  @Nonnull
  public ESuccess deleteEntry (@Nonnull final IPeppolParticipantIdentifier aParticipantID,
                               @Nonnull final PYPDocumentMetaData aMetaData) throws IOException
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    ValueEnforcer.notNull (aMetaData, "MetaData");

    return m_aLucene.runAtomic ( () -> {
      final List <Document> aDocuments = new ArrayList <> ();

      // Get all documents to be marked as deleted
      final IndexSearcher aSearcher = m_aLucene.getSearcher ();
      if (aSearcher != null)
        aSearcher.search (new TermQuery (_createParticipantTerm (aParticipantID)),
                          new AllDocumentsCollector (m_aLucene, aDoc -> aDocuments.add (aDoc)));

      if (!aDocuments.isEmpty ())
      {
        // Mark document as deleted
        for (final Document aDocument : aDocuments)
          aDocument.add (FIELD_VALUE_DELETED);

        // Update the documents
        m_aLucene.updateDocuments (_createParticipantTerm (aParticipantID), aDocuments);
      }

      s_aLogger.info ("Marked " + aDocuments.size () + " Lucene documents as deleted");
      AuditHelper.onAuditExecuteSuccess ("pyp-indexer-delete",
                                         aParticipantID.getURIEncoded (),
                                         Integer.valueOf (aDocuments.size ()),
                                         aMetaData);
    });
  }

  @Nonnull
  public ESuccess createOrUpdateEntry (@Nonnull final IPeppolParticipantIdentifier aParticipantID,
                                       @Nonnull final PYPExtendedBusinessInformation aExtBI,
                                       @Nonnull final PYPDocumentMetaData aMetaData) throws IOException
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    ValueEnforcer.notNull (aExtBI, "ExtBI");
    ValueEnforcer.notNull (aMetaData, "MetaData");

    return m_aLucene.runAtomic ( () -> {
      final List <Document> aDocs = new ArrayList <> ();

      final BusinessInformationType aBI = aExtBI.getBusinessInformation ();
      for (final EntityType aEntity : aBI.getEntity ())
      {
        // Convert entity to Lucene document
        final Document aDoc = new Document ();
        final StringBuilder aSB = new StringBuilder ();

        aDoc.add (new StringField (CPYPStorage.FIELD_PARTICIPANTID, aParticipantID.getURIEncoded (), Store.YES));
        aSB.append (aParticipantID.getURIEncoded ()).append (' ');

        // Add all document types to all documents
        for (final IDocumentTypeIdentifier aDocTypeID : aExtBI.getAllDocumentTypeIDs ())
        {
          final String sDocTypeID = IdentifierHelper.getIdentifierURIEncoded (aDocTypeID);
          aDoc.add (new StringField (CPYPStorage.FIELD_DOCUMENT_TYPE_ID, sDocTypeID, Store.YES));
          aSB.append (sDocTypeID).append (' ');
        }

        if (aEntity.getCountryCode () != null)
        {
          aDoc.add (new StringField (CPYPStorage.FIELD_COUNTRY_CODE, aEntity.getCountryCode (), Store.YES));
          aSB.append (aEntity.getCountryCode ()).append (' ');
        }

        if (aEntity.getName () != null)
        {
          aDoc.add (new TextField (CPYPStorage.FIELD_NAME, aEntity.getName (), Store.YES));
          aSB.append (aEntity.getName ()).append (' ');
        }

        if (aEntity.getGeoInfo () != null)
        {
          aDoc.add (new TextField (CPYPStorage.FIELD_GEOINFO, aEntity.getGeoInfo (), Store.YES));
          aSB.append (aEntity.getGeoInfo ()).append (' ');
        }

        for (final IdentifierType aIdentifier : aEntity.getIdentifier ())
        {
          aDoc.add (new TextField (CPYPStorage.FIELD_IDENTIFIER_TYPE, aIdentifier.getType (), Store.YES));
          aSB.append (aIdentifier.getType ()).append (' ');

          aDoc.add (new TextField (CPYPStorage.FIELD_IDENTIFIER, aIdentifier.getValue (), Store.YES));
          aSB.append (aIdentifier.getValue ()).append (' ');
        }

        if (aEntity.getFreeText () != null)
        {
          aDoc.add (new TextField (CPYPStorage.FIELD_FREETEXT, aEntity.getFreeText (), Store.YES));
          aSB.append (aEntity.getFreeText ()).append (' ');
        }

        // Add the "all" field
        aDoc.add (new TextField (CPYPStorage.FIELD_ALL_FIELDS, aSB.toString (), Store.NO));

        // Add meta data (not part of the "all field" field!)
        aDoc.add (new LongField (CPYPStorage.FIELD_METADATA_CREATIONDT, aMetaData.getCreationDTMillis (), Store.YES));
        aDoc.add (new StringField (CPYPStorage.FIELD_METADATA_OWNERID, aMetaData.getOwnerID (), Store.YES));
        aDoc.add (new StringField (CPYPStorage.FIELD_METADATA_REQUESTING_HOST,
                                   aMetaData.getRequestingHost (),
                                   Store.YES));

        aDocs.add (aDoc);
      }

      if (!aDocs.isEmpty ())
      {
        // Add "group end" marker
        CollectionHelper.getLastElement (aDocs)
                        .add (new Field (CPYPStorage.FIELD_GROUP_END, VALUE_GROUP_END, TYPE_GROUP_END));
      }

      // Delete all existing documents of the participant ID
      m_aLucene.deleteDocuments (_createParticipantTerm (aParticipantID));

      // Add to index
      m_aLucene.updateDocuments (null, aDocs);

      s_aLogger.info ("Added " + aDocs.size () + " Lucene documents");
      AuditHelper.onAuditExecuteSuccess ("pyp-indexer-create",
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
   * @param aConsumer
   *        The consumer of the {@link PYPStoredDocument} objects.
   * @throws IOException
   *         On Lucene error
   * @see #getAllDocuments(Query)
   */
  @Nonnull
  public void searchAllDocuments (@Nonnull final Query aQuery,
                                  @Nonnull final Consumer <PYPStoredDocument> aConsumer) throws IOException
  {
    ValueEnforcer.notNull (aQuery, "Query");
    ValueEnforcer.notNull (aConsumer, "Consumer");

    m_aLucene.runAtomic ( () -> {
      final IndexSearcher aSearcher = m_aLucene.getSearcher ();
      if (aSearcher != null)
      {
        if (s_aLogger.isDebugEnabled ())
          s_aLogger.debug ("Searching Lucene: " + aQuery);

        // Search all documents, convert them to StoredDocument and pass them to
        // the provided consumer
        aSearcher.search (aQuery,
                          new AllDocumentsCollector (m_aLucene,
                                                     aDoc -> aConsumer.accept (PYPStoredDocument.create (aDoc))));
      }
      else
        s_aLogger.warn ("Failed to obtain IndexSearcher");
    });
  }

  /**
   * Get all {@link PYPStoredDocument} objects matching the provided query. This
   * is a specialization of {@link #searchAllDocuments(Query, Consumer)}.
   *
   * @param aQuery
   *        The query to be executed. May not be <code>null</code>.
   * @return A non-<code>null</code> but maybe empty list of matching documents
   */
  @Nonnull
  @ReturnsMutableCopy
  public List <PYPStoredDocument> getAllDocuments (@Nonnull final Query aQuery)
  {
    final List <PYPStoredDocument> aTargetList = new ArrayList <> ();
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
  public List <PYPStoredDocument> getAllDeletedDocuments ()
  {
    return getAllDocuments (new TermQuery (new Term (CPYPStorage.FIELD_DELETED)));
  }

  @Nonnull
  public List <PYPStoredDocument> getAllDocumentsOfParticipant (@Nonnull final IPeppolParticipantIdentifier aParticipantID)
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    return getAllDocuments (new TermQuery (_createParticipantTerm (aParticipantID)));
  }

  @Nonnull
  public List <PYPStoredDocument> getAllDocumentsOfCountryCode (@Nonnull final String sCountryCode)
  {
    ValueEnforcer.notNull (sCountryCode, "CountryCode");
    return getAllDocuments (new TermQuery (new Term (CPYPStorage.FIELD_COUNTRY_CODE, sCountryCode)));
  }

  /**
   * Group the passed document list by participant ID
   *
   * @param aDocs
   *        The document list to group.
   * @return A non-<code>null</code> LinkedHashMap with the results. Order is
   *         like the input order.
   */
  @Nonnull
  public static IMultiMapListBased <String, PYPStoredDocument> getGroupedByParticipantID (@Nonnull final List <PYPStoredDocument> aDocs)
  {
    final MultiLinkedHashMapArrayListBased <String, PYPStoredDocument> ret = new MultiLinkedHashMapArrayListBased <> ();
    for (final PYPStoredDocument aDoc : aDocs)
      ret.putSingle (aDoc.getParticipantID (), aDoc);
    return ret;
  }
}
