/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.time.Month;
import java.util.List;

import javax.annotation.Nonnull;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.commons.collection.CollectionHelper;
import com.helger.datetime.PDTFactory;
import com.helger.pd.businesscard.PDBusinessCardType;
import com.helger.pd.businesscard.PDBusinessEntityType;
import com.helger.pd.businesscard.PDContactType;
import com.helger.pd.businesscard.PDExtendedBusinessCard;
import com.helger.pd.businesscard.PDIdentifierType;
import com.helger.pd.indexer.PDIndexerTestRule;
import com.helger.pd.indexer.lucene.PDLucene;
import com.helger.peppol.identifier.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppol.identifier.peppol.participant.IPeppolParticipantIdentifier;
import com.helger.peppol.identifier.peppol.participant.PeppolParticipantIdentifier;

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
  private static PDExtendedBusinessCard _createMockBI (@Nonnull final IPeppolParticipantIdentifier aParticipantID)
  {
    final PDBusinessCardType aBI = new PDBusinessCardType ();
    {
      final PDIdentifierType aID = new PDIdentifierType ();
      aID.setScheme (IPeppolParticipantIdentifier.DEFAULT_SCHEME);
      aID.setValue ("9915:mock");
      aBI.setParticipantIdentifier (aID);
    }
    {
      final PDBusinessEntityType aEntity = new PDBusinessEntityType ();
      aEntity.setCountryCode ("AT");
      aEntity.setRegistrationDate (PDTFactory.createLocalDate (2015, Month.JULY, 6));
      aEntity.setName ("Philip's mock PEPPOL receiver");
      aEntity.setGeographicalInformation ("Vienna");

      for (int i = 0; i < 10; ++i)
      {
        final PDIdentifierType aID = new PDIdentifierType ();
        aID.setScheme ("scheme" + i);
        aID.setValue ("value" + i);
        aEntity.addIdentifier (aID);
      }

      aEntity.addWebsiteURI ("http://www.peppol.eu");

      final PDContactType aBC = new PDContactType ();
      aBC.setType ("support");
      aBC.setName ("BC name");
      aBC.setEmail ("test@example.org");
      aBC.setPhoneNumber ("12345");
      aEntity.addContact (aBC);

      aEntity.setAdditionalInformation ("This is a mock entry for testing purposes only");
      aBI.addBusinessEntity (aEntity);
    }
    {
      final PDBusinessEntityType aEntity = new PDBusinessEntityType ();
      aEntity.setCountryCode ("NO");
      aEntity.setName ("Entity2");

      PDIdentifierType aID = new PDIdentifierType ();
      aID.setScheme ("mock");
      aID.setValue ("12345678");
      aEntity.addIdentifier (aID);

      aID = new PDIdentifierType ();
      aID.setScheme ("provided");
      aID.setValue (aParticipantID.getURIEncoded ());
      aEntity.addIdentifier (aID);

      aEntity.setAdditionalInformation ("This is another mock entry for testing purposes only");
      aBI.addBusinessEntity (aEntity);
    }
    return new PDExtendedBusinessCard (aBI,
                                       CollectionHelper.newList (EPredefinedDocumentTypeIdentifier.INVOICE_T010_BIS5A_V20));
  }

  @Test
  public void testGetAllDocumentsOfParticipant () throws IOException
  {
    final PeppolParticipantIdentifier aParticipantID = PeppolParticipantIdentifier.createWithDefaultScheme ("0088:test");
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
        assertEquals (PDTFactory.createLocalDate (2015, Month.JULY, 6), aDoc1.getRegistrationDate ());
        assertNotNull (aDoc1.getName ());
        assertEquals ("Vienna", aDoc1.getGeoInfo ());

        assertEquals (10, aDoc1.getIdentifierCount ());
        for (int i = 0; i < aDoc1.getIdentifierCount (); ++i)
        {
          assertEquals ("scheme" + i, aDoc1.getIdentifierAtIndex (i).getScheme ());
          assertEquals ("value" + i, aDoc1.getIdentifierAtIndex (i).getValue ());
        }

        assertEquals (1, aDoc1.getWebsiteURICount ());
        assertEquals ("http://www.peppol.eu", aDoc1.getWebsiteURIAtIndex (0));

        assertEquals (1, aDoc1.getContactCount ());
        assertEquals ("support", aDoc1.getContactAtIndex (0).getType ());
        assertEquals ("BC name", aDoc1.getContactAtIndex (0).getName ());
        assertEquals ("test@example.org", aDoc1.getContactAtIndex (0).getEmail ());
        assertEquals ("12345", aDoc1.getContactAtIndex (0).getPhone ());

        assertEquals ("This is a mock entry for testing purposes only", aDoc1.getAdditionalInformation ());
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
    final PeppolParticipantIdentifier aParticipantID = PeppolParticipantIdentifier.createWithDefaultScheme ("0088:test");
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
