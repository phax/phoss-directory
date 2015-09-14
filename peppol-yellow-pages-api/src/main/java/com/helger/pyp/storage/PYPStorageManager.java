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

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SimpleCollector;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.state.ESuccess;
import com.helger.peppol.identifier.participant.IPeppolParticipantIdentifier;
import com.helger.photon.basic.security.audit.AuditHelper;
import com.helger.pyp.businessinformation.BusinessInformationType;
import com.helger.pyp.businessinformation.EntityType;
import com.helger.pyp.businessinformation.IdentifierType;
import com.helger.pyp.lucene.PYPLucene;

/**
 * The global storage manager that wraps the used Lucene index.
 *
 * @author Philip Helger
 */
public final class PYPStorageManager implements Closeable
{
  public final class AllDocumentsCollector extends SimpleCollector
  {
    private final Consumer <Document> m_aConsumer;

    public AllDocumentsCollector (@Nonnull final Consumer <Document> aConsumer)
    {
      m_aConsumer = aConsumer;
    }

    public boolean needsScores ()
    {
      return false;
    }

    @Override
    public void collect (final int doc) throws IOException
    {
      final Document aDoc = m_aLucene.getDocument (doc);
      m_aConsumer.accept (aDoc);
    }
  }

  private static final Logger s_aLogger = LoggerFactory.getLogger (PYPStorageManager.class);

  private static final IntField FIELD_VALUE_DELETED = new IntField (CPYPStorage.FIELD_DELETED, 1, Store.NO);

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
  private static Term _createTerm (@Nonnull final IPeppolParticipantIdentifier aParticipantID)
  {
    return new Term (CPYPStorage.FIELD_PARTICIPANTID, aParticipantID.getURIEncoded ());
  }

  private static Query _andNotDeleted (@Nonnull final Query aQuery)
  {
    return new BooleanQuery.Builder ().add (aQuery, Occur.MUST)
                                      .add (new TermQuery (new Term (CPYPStorage.FIELD_DELETED)), Occur.MUST_NOT)
                                      .build ();
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
        final TopDocs aTopDocs = aSearcher.search (_andNotDeleted (new TermQuery (new Term (CPYPStorage.FIELD_PARTICIPANTID,
                                                                                            aParticipantID.getURIEncoded ()))),
                                                   1);
        if (aTopDocs.totalHits > 0)
          return Boolean.TRUE;
      }
      return Boolean.FALSE;
    }).booleanValue ();
  }

  @Nonnull
  public ESuccess deleteEntry (@Nonnull final IPeppolParticipantIdentifier aParticipantID,
                               @Nonnull @Nonempty final String sOwnerID) throws IOException
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");

    return m_aLucene.runAtomic ( () -> {
      final List <Document> aDocuments = new ArrayList <> ();

      // Get all documents to be marked as deleted
      final IndexSearcher aSearcher = m_aLucene.getSearcher ();
      if (aSearcher != null)
        aSearcher.search (new TermQuery (_createTerm (aParticipantID)),
                          new AllDocumentsCollector (aDoc -> aDocuments.add (aDoc)));

      if (!aDocuments.isEmpty ())
      {
        // Mark document as deleted
        for (final Document aDocument : aDocuments)
          aDocument.add (FIELD_VALUE_DELETED);

        // Update the documents
        m_aLucene.updateDocuments (_createTerm (aParticipantID), aDocuments);
      }

      s_aLogger.info ("Marked " + aDocuments.size () + " Lucene documents as deleted");
      AuditHelper.onAuditExecuteSuccess ("pyp-indexer-delete",
                                         aParticipantID.getURIEncoded (),
                                         Integer.valueOf (aDocuments.size ()),
                                         sOwnerID);
    });
  }

  @Nonnull
  public ESuccess createOrUpdateEntry (@Nonnull final IPeppolParticipantIdentifier aParticipantID,
                                       @Nonnull final BusinessInformationType aBI,
                                       @Nonnull @Nonempty final String sOwnerID) throws IOException
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");

    return m_aLucene.runAtomic ( () -> {
      // Delete all existing documents of the participant ID
      m_aLucene.deleteDocuments (_createTerm (aParticipantID));

      for (final EntityType aEntity : aBI.getEntity ())
      {
        // Convert entity to Lucene document
        final Document aDoc = new Document ();
        aDoc.add (new StringField (CPYPStorage.FIELD_PARTICIPANTID, aParticipantID.getURIEncoded (), Store.YES));
        aDoc.add (new StringField (CPYPStorage.FIELD_OWNERID, sOwnerID, Store.YES));
        if (aEntity.getCountryCode () != null)
          aDoc.add (new StringField (CPYPStorage.FIELD_COUNTRY_CODE, aEntity.getCountryCode (), Store.YES));
        if (aEntity.getName () != null)
          aDoc.add (new TextField (CPYPStorage.FIELD_NAME, aEntity.getName (), Store.YES));
        if (aEntity.getGeoInfo () != null)
          aDoc.add (new TextField (CPYPStorage.FIELD_GEOINFO, aEntity.getGeoInfo (), Store.YES));

        for (final IdentifierType aIdentifier : aEntity.getIdentifier ())
        {
          aDoc.add (new TextField (CPYPStorage.FIELD_IDENTIFIER_TYPE, aIdentifier.getType (), Store.YES));
          aDoc.add (new TextField (CPYPStorage.FIELD_IDENTIFIER, aIdentifier.getValue (), Store.YES));
        }
        if (aEntity.getFreeText () != null)
          aDoc.add (new TextField (CPYPStorage.FIELD_FREETEXT, aEntity.getFreeText (), Store.YES));

        // Add to index
        m_aLucene.updateDocument (null, aDoc);
      }

      s_aLogger.info ("Added " + aBI.getEntityCount () + " Lucene documents");
      AuditHelper.onAuditExecuteSuccess ("pyp-indexer-create",
                                         aParticipantID.getURIEncoded (),
                                         Integer.valueOf (aBI.getEntityCount ()),
                                         sOwnerID);
    });
  }

  /**
   * Search all documents matching the passed query and pass the result on to
   * the provided {@link Consumer}.
   *
   * @param aQuery
   *        Query to execute. May not be <code>null</code>-
   * @param aConsumer
   *        The consumer of the {@link StoredDocument} objects.
   * @throws IOException
   *         On Lucene error
   * @see #getAllDocuments(Query)
   */
  @Nonnull
  public void searchAllDocuments (@Nonnull final Query aQuery,
                                  @Nonnull final Consumer <StoredDocument> aConsumer) throws IOException
  {
    ValueEnforcer.notNull (aQuery, "Query");
    ValueEnforcer.notNull (aConsumer, "Consumer");

    m_aLucene.runAtomic ( () -> {
      final IndexSearcher aSearcher = m_aLucene.getSearcher ();
      if (aSearcher != null)
      {
        // Search all documents, convert them to StoredDocument and pass them to
        // the provided consumer
        aSearcher.search (aQuery, new AllDocumentsCollector (aDoc -> aConsumer.accept (StoredDocument.create (aDoc))));
      }
    });
  }

  /**
   * Get all {@link StoredDocument} objects matching the provided query. This is
   * a specialization of {@link #searchAllDocuments(Query, Consumer)}.
   *
   * @param aQuery
   *        The query to be executed. May not be <code>null</code>.
   * @return A non-<code>null</code> but maybe empty list of matching documents
   * @throws IOException
   *         On Lucene error
   */
  @Nonnull
  @ReturnsMutableCopy
  public List <StoredDocument> getAllDocuments (@Nonnull final Query aQuery) throws IOException
  {
    final List <StoredDocument> aTargetList = new ArrayList <> ();
    searchAllDocuments (aQuery, aDoc -> aTargetList.add (aDoc));
    return aTargetList;
  }

  @Nonnull
  public List <StoredDocument> getAllDeletedDocuments () throws IOException
  {
    return getAllDocuments (new TermQuery (new Term (CPYPStorage.FIELD_DELETED)));
  }

  @Nonnull
  public List <StoredDocument> getAllDocumentsOfParticipant (@Nonnull final IPeppolParticipantIdentifier aParticipantID) throws IOException
  {
    return getAllDocuments (new TermQuery (_createTerm (aParticipantID)));
  }

  @Nonnull
  public List <StoredDocument> getAllDocumentsOfCountryCode (@Nonnull final String sCountryCode) throws IOException
  {
    ValueEnforcer.notNull (sCountryCode, "CountryCode");
    return getAllDocuments (new TermQuery (new Term (CPYPStorage.FIELD_COUNTRY_CODE, sCountryCode)));
  }
}
