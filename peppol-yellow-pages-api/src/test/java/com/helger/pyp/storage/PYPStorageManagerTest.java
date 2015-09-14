package com.helger.pyp.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.peppol.identifier.participant.IPeppolParticipantIdentifier;
import com.helger.peppol.identifier.participant.SimpleParticipantIdentifier;
import com.helger.pyp.businessinformation.BusinessInformationType;
import com.helger.pyp.businessinformation.EntityType;
import com.helger.pyp.businessinformation.IdentifierType;
import com.helger.pyp.lucene.PYPLucene;
import com.helger.pyp.mock.PYPAPITestRule;

/**
 * Test class for class {@link PYPStorageManager}.
 *
 * @author Philip Helger
 */
public final class PYPStorageManagerTest
{
  @Rule
  public final TestRule m_aRule = new PYPAPITestRule ();

  @Nonnull
  private static BusinessInformationType _createMockBI (@Nonnull final IPeppolParticipantIdentifier aParticipantID)
  {
    final BusinessInformationType ret = new BusinessInformationType ();
    {
      final EntityType aEntity = new EntityType ();
      aEntity.setCountryCode ("AT");
      aEntity.setName ("Philip's mock PEPPOL receiver");
      aEntity.setGeoInfo ("Rome");
      IdentifierType aID = new IdentifierType ();
      aID.setType ("mock");
      aID.setValue ("12345678");
      aEntity.addIdentifier (aID);
      aID = new IdentifierType ();
      aID.setType ("provided");
      aID.setValue (aParticipantID.getURIEncoded ());
      aEntity.addIdentifier (aID);
      aEntity.setFreeText ("This is a mock entry for testing purposes only");
      ret.addEntity (aEntity);
    }
    {
      final EntityType aEntity = new EntityType ();
      aEntity.setCountryCode ("NO");
      for (int i = 0; i < 10; ++i)
      {
        final IdentifierType aID = new IdentifierType ();
        aID.setType ("type" + i);
        aID.setValue ("value" + i);
        aEntity.addIdentifier (aID);
      }
      aEntity.setFreeText ("This is another mock entry for testing purposes only");
      ret.addEntity (aEntity);
    }
    return ret;
  }

  @Test
  public void testGetAllDocumentsOfParticipant () throws IOException
  {
    final SimpleParticipantIdentifier aParticipantID = SimpleParticipantIdentifier.createWithDefaultScheme ("0088:test");
    final String sOwnerID = "junit-test";
    try (PYPStorageManager aMgr = new PYPStorageManager (new PYPLucene ()))
    {
      aMgr.createOrUpdateEntry (aParticipantID, _createMockBI (aParticipantID), sOwnerID);
      final List <PYPStoredDocument> aDocs = aMgr.getAllDocumentsOfParticipant (aParticipantID);
      assertEquals (2, aDocs.size ());
      final PYPStoredDocument aDoc1 = aDocs.get (1);
      assertEquals (aParticipantID.getURIEncoded (), aDoc1.getParticipantID ());
      assertEquals (sOwnerID, aDoc1.getOwnerID ());
      assertEquals ("NO", aDoc1.getCountryCode ());
      assertNull (aDoc1.getName ());
      assertNull (aDoc1.getGeoInfo ());
      assertEquals (10, aDoc1.getIdentifierCount ());
      for (int i = 0; i < aDoc1.getIdentifierCount (); ++i)
      {
        assertEquals ("type" + i, aDoc1.getIdentifierAtIndex (i).getType ());
        assertEquals ("value" + i, aDoc1.getIdentifierAtIndex (i).getValue ());
      }
      assertEquals ("This is another mock entry for testing purposes only", aDoc1.getFreeText ());
      assertFalse (aDoc1.isDeleted ());

      // Finally delete the entry again
      aMgr.deleteEntry (aParticipantID, sOwnerID);
    }
  }

  @Test
  public void testGetAllDocumentsOfCountryCode () throws IOException
  {
    final SimpleParticipantIdentifier aParticipantID = SimpleParticipantIdentifier.createWithDefaultScheme ("0088:test");
    final String sOwnerID = "junit-test";
    try (PYPStorageManager aMgr = new PYPStorageManager (new PYPLucene ()))
    {
      aMgr.createOrUpdateEntry (aParticipantID, _createMockBI (aParticipantID), sOwnerID);
      List <PYPStoredDocument> aDocs = aMgr.getAllDocumentsOfCountryCode ("");
      assertEquals (0, aDocs.size ());

      aDocs = aMgr.getAllDocumentsOfCountryCode ("NO");
      assertEquals (1, aDocs.size ());
      final PYPStoredDocument aDoc = aDocs.get (0);
      assertEquals (aParticipantID.getURIEncoded (), aDoc.getParticipantID ());
      assertEquals (sOwnerID, aDoc.getOwnerID ());
      assertEquals ("NO", aDoc.getCountryCode ());

      // Finally delete the entry again
      aMgr.deleteEntry (aParticipantID, sOwnerID);
    }
  }
}
