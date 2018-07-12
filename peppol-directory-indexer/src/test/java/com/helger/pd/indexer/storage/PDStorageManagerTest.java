/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
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

import javax.annotation.Nonnull;

import org.apache.lucene.search.TermQuery;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.datetime.PDTFactory;
import com.helger.pd.businesscard.PDExtendedBusinessCard;
import com.helger.pd.businesscard.generic.PDBusinessCard;
import com.helger.pd.businesscard.generic.PDBusinessEntity;
import com.helger.pd.businesscard.generic.PDContact;
import com.helger.pd.businesscard.generic.PDIdentifier;
import com.helger.pd.businesscard.generic.PDName;
import com.helger.pd.indexer.PDIndexerTestRule;
import com.helger.pd.indexer.lucene.PDLucene;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.indexer.storage.field.PDField;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.identifier.peppol.PeppolIdentifierHelper;
import com.helger.peppol.identifier.peppol.doctype.EPredefinedDocumentTypeIdentifier;

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
    aBI.setParticipantIdentifier (new PDIdentifier (PeppolIdentifierHelper.DEFAULT_PARTICIPANT_SCHEME, "9915:mock"));
    {
      final PDBusinessEntity aEntity = new PDBusinessEntity ();
      aEntity.setCountryCode ("AT");
      aEntity.setRegistrationDate (PDTFactory.createLocalDate (2015, Month.JULY, 6));
      aEntity.names ().add (new PDName ("Philip's mock PEPPOL receiver"));
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
      aEntity.names ().add (new PDName ("Entity2", "no"));

      aEntity.identifiers ().add (new PDIdentifier ("mock", "12345678"));
      aEntity.identifiers ().add (new PDIdentifier ("provided", aParticipantID.getURIEncoded ()));

      aEntity.setAdditionalInfo ("This is another mock entry for testing purposes only");
      aBI.businessEntities ().add (aEntity);
    }
    return new PDExtendedBusinessCard (aBI,
                                       new CommonsArrayList <> (EPredefinedDocumentTypeIdentifier.INVOICE_T010_BIS5A_V20));
  }

  @Test
  public void testGetAllDocumentsOfParticipant () throws IOException
  {
    final IParticipantIdentifier aParticipantID = PDMetaManager.getIdentifierFactory ()
                                                               .createParticipantIdentifier ("myscheme", "0088:test");
    try (PDStorageManager aMgr = new PDStorageManager (new PDLucene ()))
    {
      final PDStoredMetaData aMetaData = _createMockMetaData ();
      aMgr.createOrUpdateEntry (aParticipantID, _createMockBI (aParticipantID), aMetaData);
      try
      {
        final ICommonsList <PDStoredBusinessEntity> aDocs = aMgr.getAllDocumentsOfParticipant (aParticipantID);
        assertEquals (2, aDocs.size ());
        final PDStoredBusinessEntity aDoc1 = aDocs.get (0);
        assertEquals (aParticipantID, aDoc1.getParticipantID ());
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
    final IParticipantIdentifier aParticipantID = PDMetaManager.getIdentifierFactory ()
                                                               .createParticipantIdentifier ("myscheme", "0088:test");
    try (PDStorageManager aMgr = new PDStorageManager (new PDLucene ()))
    {
      final PDStoredMetaData aMetaData = _createMockMetaData ();
      aMgr.createOrUpdateEntry (aParticipantID, _createMockBI (aParticipantID), aMetaData);
      try
      {
        // No country - no fields
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
        aMgr.deleteEntry (aParticipantID, aMetaData);
      }
    }
  }
}
