/*
 * Copyright (C) 2015-2024 Philip Helger (www.helger.com)
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.time.Month;

import javax.annotation.Nonnull;

import org.apache.lucene.search.TermQuery;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.datetime.PDTFactory;
import com.helger.pd.indexer.PDIndexerTestRule;
import com.helger.pd.indexer.businesscard.PDExtendedBusinessCard;
import com.helger.pd.indexer.lucene.PDLucene;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.indexer.storage.field.PDField;
import com.helger.peppol.businesscard.generic.PDBusinessCard;
import com.helger.peppol.businesscard.generic.PDBusinessEntity;
import com.helger.peppol.businesscard.generic.PDContact;
import com.helger.peppol.businesscard.generic.PDIdentifier;
import com.helger.peppol.businesscard.generic.PDName;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;

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
  private static PDStoredMetaData _createMockMetaData ()
  {
    return new PDStoredMetaData (PDTFactory.getCurrentLocalDateTime (), "junittest", "localhost");
  }

  @Nonnull
  private static PDExtendedBusinessCard _createMockBI (@Nonnull final IParticipantIdentifier aParticipantID)
  {
    final PDBusinessCard aBI = new PDBusinessCard ();
    aBI.setParticipantIdentifier (new PDIdentifier (aParticipantID.getScheme (), aParticipantID.getValue ()));
    {
      final PDBusinessEntity aEntity = new PDBusinessEntity ();
      aEntity.setCountryCode ("AT");
      aEntity.setRegistrationDate (PDTFactory.createLocalDate (2015, Month.JULY, 6));
      aEntity.names ().add (new PDName ("Philip's mock Peppol receiver"));
      aEntity.setGeoInfo ("Vienna");

      for (int i = 0; i < 10; ++i)
        aEntity.identifiers ().add (new PDIdentifier ("scheme" + i, "value" + i));
      aEntity.websiteURIs ().add ("http://www.peppol.eu");
      aEntity.contacts ().add (new PDContact ("support", "BC name", "12345", "test@example.org"));

      aEntity.setAdditionalInfo ("This is a mock entry for testing purposes only");
      aBI.businessEntities ().add (aEntity);
    }
    {
      final PDBusinessEntity aEntity = new PDBusinessEntity ();
      aEntity.setCountryCode ("NO");
      aEntity.names ().add (new PDName ("Entity2a", "no"));
      aEntity.names ().add (new PDName ("Entity2b", "de"));
      aEntity.names ().add (new PDName ("Entity2c", "en"));

      aEntity.setAdditionalInfo ("Mock");
      aBI.businessEntities ().add (aEntity);
    }
    return new PDExtendedBusinessCard (aBI, new CommonsArrayList <> (EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30));
  }

  @Test
  public void testGetAllDocumentsOfParticipant () throws IOException
  {
    final IParticipantIdentifier aParticipantID = PDMetaManager.getIdentifierFactory ()
                                                               .createParticipantIdentifier ("myscheme-actorid-upis", "0088:test");
    assertNotNull (aParticipantID);

    try (final PDStorageManager aMgr = new PDStorageManager (new PDLucene ()))
    {
      final PDStoredMetaData aMetaData = _createMockMetaData ();
      aMgr.createOrUpdateEntry (aParticipantID, _createMockBI (aParticipantID), aMetaData);
      try
      {
        final ICommonsList <PDStoredBusinessEntity> aDocs = aMgr.getAllDocumentsOfParticipant (aParticipantID);
        assertEquals (2, aDocs.size ());

        // Test entity 1
        final PDStoredBusinessEntity aDoc1 = aDocs.get (0);
        assertEquals (aParticipantID, aDoc1.getParticipantID ());
        assertEquals ("junittest", aDoc1.getMetaData ().getOwnerID ());
        assertEquals ("AT", aDoc1.getCountryCode ());
        assertEquals (PDTFactory.createLocalDate (2015, Month.JULY, 6), aDoc1.getRegistrationDate ());
        assertEquals (1, aDoc1.names ().size ());
        assertEquals ("Philip's mock Peppol receiver", aDoc1.names ().get (0).getName ());
        assertNull (aDoc1.names ().get (0).getLanguageCode ());
        assertEquals ("Vienna", aDoc1.getGeoInfo ());

        assertEquals (10, aDoc1.identifiers ().size ());
        for (int i = 0; i < aDoc1.identifiers ().size (); ++i)
        {
          assertEquals ("scheme" + i, aDoc1.identifiers ().get (i).getScheme ());
          assertEquals ("value" + i, aDoc1.identifiers ().get (i).getValue ());
        }

        assertEquals (1, aDoc1.websiteURIs ().size ());
        assertEquals ("http://www.peppol.eu", aDoc1.websiteURIs ().get (0));

        assertEquals (1, aDoc1.contacts ().size ());
        assertEquals ("support", aDoc1.contacts ().get (0).getType ());
        assertEquals ("BC name", aDoc1.contacts ().get (0).getName ());
        assertEquals ("test@example.org", aDoc1.contacts ().get (0).getEmail ());
        assertEquals ("12345", aDoc1.contacts ().get (0).getPhone ());

        assertEquals ("This is a mock entry for testing purposes only", aDoc1.getAdditionalInformation ());

        // Test entity 2
        final PDStoredBusinessEntity aDoc2 = aDocs.get (1);
        assertEquals (aParticipantID, aDoc2.getParticipantID ());
        assertEquals ("junittest", aDoc2.getMetaData ().getOwnerID ());
        assertEquals ("NO", aDoc2.getCountryCode ());
        assertNull (aDoc2.getRegistrationDate ());
        assertEquals (3, aDoc2.names ().size ());
        assertEquals ("Entity2a", aDoc2.names ().get (0).getName ());
        assertEquals ("no", aDoc2.names ().get (0).getLanguageCode ());
        assertEquals ("Entity2b", aDoc2.names ().get (1).getName ());
        assertEquals ("de", aDoc2.names ().get (1).getLanguageCode ());
        assertEquals ("Entity2c", aDoc2.names ().get (2).getName ());
        assertEquals ("en", aDoc2.names ().get (2).getLanguageCode ());

        assertNull (aDoc2.getGeoInfo ());
        assertEquals (0, aDoc2.identifiers ().size ());
        assertEquals (0, aDoc2.websiteURIs ().size ());
        assertEquals (0, aDoc2.contacts ().size ());

        assertEquals ("Mock", aDoc2.getAdditionalInformation ());
      }
      finally
      {
        // Finally delete the entry again
        aMgr.deleteEntry (aParticipantID, aMetaData, true);
      }
    }
  }

  @Test
  public void testGetAllDocumentsOfCountryCode () throws IOException
  {
    final IParticipantIdentifier aParticipantID = PDMetaManager.getIdentifierFactory ()
                                                               .createParticipantIdentifier ("myscheme-actorid-upis", "0088:test");
    assertNotNull (aParticipantID);

    try (final PDStorageManager aMgr = new PDStorageManager (new PDLucene ()))
    {
      final PDStoredMetaData aMetaData = _createMockMetaData ();
      aMgr.createOrUpdateEntry (aParticipantID, _createMockBI (aParticipantID), aMetaData);
      try
      {
        // No country - no docs
        ICommonsList <PDStoredBusinessEntity> aDocs = aMgr.getAllDocuments (new TermQuery (PDField.COUNTRY_CODE.getExactMatchTerm ("")),
                                                                            -1);
        assertEquals (0, aDocs.size ());

        // Search for NO
        aDocs = aMgr.getAllDocuments (new TermQuery (PDField.COUNTRY_CODE.getExactMatchTerm ("NO")), -1);
        assertEquals (1, aDocs.size ());

        final PDStoredBusinessEntity aSingleDoc = aDocs.get (0);
        assertEquals (aParticipantID, aSingleDoc.getParticipantID ());
        assertEquals ("junittest", aSingleDoc.getMetaData ().getOwnerID ());
        assertEquals ("NO", aSingleDoc.getCountryCode ());
      }
      finally
      {
        // Finally delete the entry again
        aMgr.deleteEntry (aParticipantID, aMetaData, true);
      }
    }
  }
}
