package com.helger.pd.indexer.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;

import org.joda.time.DateTimeConstants;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.commons.collection.CollectionHelper;
import com.helger.datetime.PDTFactory;
import com.helger.pd.businessinformation.PDBusinessContactType;
import com.helger.pd.businessinformation.PDBusinessInformationType;
import com.helger.pd.businessinformation.PDEntityType;
import com.helger.pd.businessinformation.PDExtendedBusinessInformation;
import com.helger.pd.businessinformation.PDIdentifierType;
import com.helger.pd.indexer.PDIndexerTestRule;
import com.helger.pd.indexer.lucene.PDLucene;
import com.helger.peppol.identifier.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppol.identifier.participant.IPeppolParticipantIdentifier;
import com.helger.peppol.identifier.participant.SimpleParticipantIdentifier;

/**
 * Test class for class {@link PDStorageManager}.
 *
 * @author Philip Helger
 */
public final class PDStorageManagerTest
{
  @Rule
  public final TestRule m_aRule = new PDIndexerTestRule ();

  @Nonnull
  private static PDDocumentMetaData _createMockMetaData ()
  {
    return new PDDocumentMetaData (PDTFactory.getCurrentLocalDateTime (), "junittest", "localhost");
  }

  @Nonnull
  private static PDExtendedBusinessInformation _createMockBI (@Nonnull final IPeppolParticipantIdentifier aParticipantID)
  {
    final PDBusinessInformationType aBI = new PDBusinessInformationType ();
    {
      final PDEntityType aEntity = new PDEntityType ();
      aEntity.setCountryCode ("AT");
      aEntity.setRegistrationDate (PDTFactory.createLocalDate (2015, DateTimeConstants.JULY, 6));
      aEntity.setName ("Philip's mock PEPPOL receiver");
      aEntity.setGeoInfo ("Vienna");

      for (int i = 0; i < 10; ++i)
      {
        final PDIdentifierType aID = new PDIdentifierType ();
        aID.setType ("type" + i);
        aID.setValue ("value" + i);
        aEntity.addIdentifier (aID);
      }

      aEntity.addWebSite ("http://www.peppol.eu");

      final PDBusinessContactType aBC = new PDBusinessContactType ();
      aBC.setDescription ("support");
      aBC.setName ("BC name");
      aBC.setEmail ("test@example.org");
      aBC.setPhoneNumber ("12345");
      aEntity.addBusinessContact (aBC);

      aEntity.setFreeText ("This is a mock entry for testing purposes only");
      aBI.addEntity (aEntity);
    }
    {
      final PDEntityType aEntity = new PDEntityType ();
      aEntity.setCountryCode ("NO");
      aEntity.setName ("Entity2");

      PDIdentifierType aID = new PDIdentifierType ();
      aID.setType ("mock");
      aID.setValue ("12345678");
      aEntity.addIdentifier (aID);

      aID = new PDIdentifierType ();
      aID.setType ("provided");
      aID.setValue (aParticipantID.getURIEncoded ());
      aEntity.addIdentifier (aID);

      aEntity.setFreeText ("This is another mock entry for testing purposes only");
      aBI.addEntity (aEntity);
    }
    return new PDExtendedBusinessInformation (aBI, CollectionHelper.newList (EPredefinedDocumentTypeIdentifier.INVOICE_T010_BIS5A_V20));
  }

  @Test
  public void testGetAllDocumentsOfParticipant () throws IOException
  {
    final SimpleParticipantIdentifier aParticipantID = SimpleParticipantIdentifier.createWithDefaultScheme ("0088:test");
    try (PDStorageManager aMgr = new PDStorageManager (new PDLucene ()))
    {
      final PDDocumentMetaData aMetaData = _createMockMetaData ();
      aMgr.createOrUpdateEntry (aParticipantID, _createMockBI (aParticipantID), aMetaData);
      try
      {
        final List <PDStoredDocument> aDocs = aMgr.getAllDocumentsOfParticipant (aParticipantID);
        assertEquals (2, aDocs.size ());
        final PDStoredDocument aDoc1 = aDocs.get (0);
        assertEquals (aParticipantID.getURIEncoded (), aDoc1.getParticipantID ());
        assertEquals ("junittest", aDoc1.getMetaData ().getOwnerID ());
        assertEquals ("AT", aDoc1.getCountryCode ());
        assertEquals (PDTFactory.createLocalDate (2015, DateTimeConstants.JULY, 6), aDoc1.getRegistrationDate ());
        assertNotNull (aDoc1.getName ());
        assertEquals ("Vienna", aDoc1.getGeoInfo ());

        assertEquals (10, aDoc1.getIdentifierCount ());
        for (int i = 0; i < aDoc1.getIdentifierCount (); ++i)
        {
          assertEquals ("type" + i, aDoc1.getIdentifierAtIndex (i).getType ());
          assertEquals ("value" + i, aDoc1.getIdentifierAtIndex (i).getValue ());
        }

        assertEquals (1, aDoc1.getWebSiteCount ());
        assertEquals ("http://www.peppol.eu", aDoc1.getWebSiteAtIndex (0));

        assertEquals (1, aDoc1.getBusinessContactCount ());
        assertEquals ("support", aDoc1.getBusinessContactAtIndex (0).getDescription ());
        assertEquals ("BC name", aDoc1.getBusinessContactAtIndex (0).getName ());
        assertEquals ("test@example.org", aDoc1.getBusinessContactAtIndex (0).getEmail ());
        assertEquals ("12345", aDoc1.getBusinessContactAtIndex (0).getPhone ());

        assertEquals ("This is a mock entry for testing purposes only", aDoc1.getFreeText ());
        assertFalse (aDoc1.isDeleted ());
      }
      finally
      {
        // Finally delete the entry again
        aMgr.deleteEntry (aParticipantID, aMetaData);
      }
    }
  }

  @Test
  public void testGetAllDocumentsOfCountryCode () throws IOException
  {
    final SimpleParticipantIdentifier aParticipantID = SimpleParticipantIdentifier.createWithDefaultScheme ("0088:test");
    try (PDStorageManager aMgr = new PDStorageManager (new PDLucene ()))
    {
      final PDDocumentMetaData aMetaData = _createMockMetaData ();
      aMgr.createOrUpdateEntry (aParticipantID, _createMockBI (aParticipantID), aMetaData);
      try
      {
        // No country - no fields
        List <PDStoredDocument> aDocs = aMgr.getAllDocumentsOfCountryCode ("");
        assertEquals (0, aDocs.size ());

        // Search for NO
        aDocs = aMgr.getAllDocumentsOfCountryCode ("NO");
        assertEquals (1, aDocs.size ());

        final PDStoredDocument aSingleDoc = aDocs.get (0);
        assertEquals (aParticipantID.getURIEncoded (), aSingleDoc.getParticipantID ());
        assertEquals ("junittest", aSingleDoc.getMetaData ().getOwnerID ());
        assertEquals ("NO", aSingleDoc.getCountryCode ());
      }
      finally
      {
        // Finally delete the entry again
        aMgr.deleteEntry (aParticipantID, aMetaData);
      }
    }
  }
}
