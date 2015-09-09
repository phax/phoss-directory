package com.helger.pyp.storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.SimpleCollector;
import org.apache.lucene.search.TermQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.state.ESuccess;
import com.helger.peppol.identifier.participant.IPeppolParticipantIdentifier;
import com.helger.pyp.businessinformation.BusinessInformationType;
import com.helger.pyp.businessinformation.EntityType;
import com.helger.pyp.businessinformation.IdentifierType;
import com.helger.pyp.lucene.PYPLucene;

/**
 * The global storage manager that wraps the used Lucene index.
 *
 * @author Philip Helger
 */
public final class PYPStorageManager
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (PYPStorageManager.class);

  private static final String FIELD_PARTICIPANTID = "participantid";
  private static final String FIELD_OWNERID = "ownerid";
  private static final String FIELD_COUNTRY = "country";
  private static final String FIELD_NAME = "name";
  private static final String FIELD_GEOINFO = "geoinfo";
  private static final String FIELD_IDENTIFIERS = "identifiers";
  private static final String FIELD_FREETEXT = "freetext";
  private static final String FIELD_DELETED = "deleted";

  private static final IntField FIELD_VALUE_DELETED = new IntField (FIELD_DELETED, 1, Store.NO);
  private static final IntField FIELD_VALUE_NOT_DELETED = new IntField (FIELD_DELETED, 0, Store.NO);

  private final PYPLucene m_aLucene;

  public PYPStorageManager (@Nonnull final PYPLucene aLucene)
  {
    m_aLucene = ValueEnforcer.notNull (aLucene, "Lucene");
  }

  @Nonnull
  private static Term _createTerm (@Nonnull final IPeppolParticipantIdentifier aParticipantID)
  {
    return new Term (FIELD_PARTICIPANTID, aParticipantID.getURIEncoded ());
  }

  public boolean containsEntry (@Nullable final IPeppolParticipantIdentifier aParticipantID)
  {
    if (aParticipantID == null)
      return false;

    return true;
  }

  public ESuccess deleteEntry (@Nonnull final IPeppolParticipantIdentifier aParticipantID) throws IOException
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");

    // Get all documents to be marked as deleted
    final List <Document> aDocuments = new ArrayList <> ();
    final IndexSearcher aSearcher = m_aLucene.getSearcher ();
    if (aSearcher != null)
    {
      aSearcher.search (new TermQuery (_createTerm (aParticipantID)), new SimpleCollector ()
      {
        public boolean needsScores ()
        {
          return false;
        }

        @Override
        public void collect (final int doc) throws IOException
        {
          final Document aDoc = m_aLucene.getReader ().document (doc);
          aDocuments.add (aDoc);
        }
      });
    }

    if (!aDocuments.isEmpty ())
    {
      // Mark document as deleted
      for (final Document aDocument : aDocuments)
        aDocument.add (FIELD_VALUE_DELETED);

      // Update the documents
      if (m_aLucene.runLocked ( () -> {
        final IndexWriter aWriter = m_aLucene.getWriter ();
        aWriter.updateDocuments (_createTerm (aParticipantID), aDocuments);
        aWriter.commit ();
      }).isFailure ())
        return ESuccess.FAILURE;
    }

    s_aLogger.info ("Marked " + aDocuments.size () + " Lucene documents as deleted");
    return ESuccess.SUCCESS;
  }

  @Nonnull
  public ESuccess createOrUpdateEntry (@Nonnull final IPeppolParticipantIdentifier aParticipantID,
                                       @Nonnull final BusinessInformationType aBI,
                                       @Nonnull @Nonempty final String sOwnerID) throws IOException
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");

    return m_aLucene.runLocked ( () -> {
      final IndexWriter aWriter = m_aLucene.getWriter ();

      // Delete all existing documents of the participant ID
      aWriter.deleteDocuments (_createTerm (aParticipantID));

      for (final EntityType aEntity : aBI.getEntity ())
      {
        // Convert entity to Lucene document
        final Document aDoc = new Document ();
        aDoc.add (new StringField (FIELD_PARTICIPANTID, aParticipantID.getURIEncoded (), Store.YES));
        aDoc.add (new StringField (FIELD_OWNERID, sOwnerID, Store.YES));
        if (aEntity.getCountryCode () != null)
          aDoc.add (new StringField (FIELD_COUNTRY, aEntity.getCountryCode (), Store.YES));
        if (aEntity.getName () != null)
          aDoc.add (new TextField (FIELD_NAME, aEntity.getName (), Store.YES));
        if (aEntity.getGeoInfo () != null)
          aDoc.add (new TextField (FIELD_GEOINFO, aEntity.getGeoInfo (), Store.YES));

        {
          // Combine all identifiers into a single text field
          final StringBuilder aIdentifierTexts = new StringBuilder ();
          for (final IdentifierType aIdentifier : aEntity.getIdentifier ())
          {
            if (aIdentifierTexts.length () > 0)
              aIdentifierTexts.append ('\n');
            aIdentifierTexts.append (aIdentifier.getType ()).append ('\n').append (aIdentifier.getValue ());
          }
          if (aIdentifierTexts.length () > 0)
            aDoc.add (new TextField (FIELD_IDENTIFIERS, aIdentifierTexts.toString (), Store.YES));
          aDoc.add (FIELD_VALUE_NOT_DELETED);
        }
        if (aEntity.getFreeText () != null)
          aDoc.add (new TextField (FIELD_FREETEXT, aEntity.getFreeText (), Store.YES));

        // Add to index
        aWriter.addDocument (aDoc);
      }

      // Finally commit
      aWriter.commit ();

      s_aLogger.info ("Added " + aBI.getEntityCount () + " Lucene documents");
    });
  }
}
