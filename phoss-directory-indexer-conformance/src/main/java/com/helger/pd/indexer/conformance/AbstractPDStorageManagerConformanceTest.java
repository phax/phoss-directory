/*
 * Copyright (C) 2026 Philip Helger (www.helger.com)
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
package com.helger.pd.indexer.conformance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.Month;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.helger.base.io.stream.StreamHelper;
import com.helger.base.state.ESuccess;
import com.helger.collection.commons.ICommonsList;
import com.helger.datetime.helper.PDTFactory;
import com.helger.pd.indexer.searchindex.IPDIndex;
import com.helger.pd.indexer.searchindex.query.PDIndexQueryMatchAll;
import com.helger.pd.indexer.storage.PDStorageManager;
import com.helger.pd.indexer.storage.PDStoredBusinessEntity;
import com.helger.pd.indexer.storage.PDStoredMetaData;
import com.helger.pd.indexer.storage.field.PDField;
import com.helger.peppolid.IParticipantIdentifier;

import jakarta.annotation.Nullable;

/**
 * The conformance test suite of {@link PDStorageManager} on top of an implementation of
 * {@link IPDIndex}. Derive from this class, implement {@link #createIndex()} and add the
 * {@link org.junit.Rule} that the implementation needs.
 *
 * @author Philip Helger
 * @since 0.16.0
 */
public abstract class AbstractPDStorageManagerConformanceTest
{
  private PDStorageManager m_aStorageMgr;
  private IParticipantIdentifier m_aParticipantID;

  /**
   * Create the search index to be tested. This method is called before every test method.
   *
   * @return The search index to be tested. May not be <code>null</code>.
   * @throws IOException
   *         On index error
   */
  @NonNull
  protected abstract IPDIndex createIndex () throws IOException;

  /**
   * @return The storage manager under test. Never <code>null</code> inside a test method.
   */
  @NonNull
  protected final PDStorageManager storageMgr ()
  {
    return m_aStorageMgr;
  }

  @Before
  public final void beforeStorageManagerConformanceTest () throws IOException
  {
    m_aParticipantID = PDConformanceTestData.createParticipantID ();
    assertNotNull (m_aParticipantID);

    final IPDIndex aIndex = createIndex ();
    assertNotNull (aIndex);

    // Every test starts with an empty index
    aIndex.deleteDocuments (PDIndexQueryMatchAll.INSTANCE);
    m_aStorageMgr = new PDStorageManager (aIndex);
    assertEquals (0, m_aStorageMgr.getContainedParticipantCount ());
  }

  @After
  public final void afterStorageManagerConformanceTest ()
  {
    StreamHelper.close (m_aStorageMgr);
    m_aStorageMgr = null;
  }

  @Nullable
  private static PDStoredBusinessEntity _findByCountry (@NonNull final ICommonsList <PDStoredBusinessEntity> aDocs,
                                                        @NonNull final String sCountryCode)
  {
    // The order in which the search index returns the entities of a participant is not part of the
    // IPDIndex contract
    return aDocs.findFirst (x -> sCountryCode.equals (x.getCountryCode ()));
  }

  @Test
  public void testCreateAndGetAllDocumentsOfParticipant () throws IOException
  {
    final PDStoredMetaData aMetaData = PDConformanceTestData.createMockMetaData ();
    assertEquals (ESuccess.SUCCESS,
                  m_aStorageMgr.createOrUpdateEntry (m_aParticipantID,
                                                     PDConformanceTestData.createMockBusinessCard (m_aParticipantID),
                                                     aMetaData));

    assertTrue (m_aStorageMgr.containsEntry (m_aParticipantID));

    final ICommonsList <PDStoredBusinessEntity> aDocs = m_aStorageMgr.getAllDocumentsOfParticipant (m_aParticipantID);
    assertEquals (2, aDocs.size ());

    // Entity 1 - a single name without a language
    final PDStoredBusinessEntity aDoc1 = _findByCountry (aDocs, "AT");
    assertNotNull (aDoc1);
    assertEquals (m_aParticipantID, aDoc1.getParticipantID ());
    assertEquals ("junittest", aDoc1.getMetaData ().getOwnerID ());
    assertEquals (PDTFactory.createLocalDate (2015, Month.JULY, 6), aDoc1.getRegistrationDate ());
    assertEquals (1, aDoc1.names ().size ());
    assertEquals ("Philip's mock Peppol receiver", aDoc1.names ().get (0).getName ());
    assertNull (aDoc1.names ().get (0).getLanguageCode ());
    assertEquals ("Vienna", aDoc1.getGeoInfo ());

    // The parallel scheme/value fields must keep their order
    assertEquals (10, aDoc1.identifiers ().size ());
    for (int i = 0; i < aDoc1.identifiers ().size (); ++i)
    {
      assertEquals ("scheme" + i, aDoc1.identifiers ().get (i).getScheme ());
      assertEquals ("value" + i, aDoc1.identifiers ().get (i).getValue ());
    }

    assertEquals (1, aDoc1.websiteURIs ().size ());
    assertEquals ("https://peppol.org", aDoc1.websiteURIs ().get (0));

    assertEquals (1, aDoc1.contacts ().size ());
    assertEquals ("support", aDoc1.contacts ().get (0).getType ());
    assertEquals ("BC name", aDoc1.contacts ().get (0).getName ());
    assertEquals ("test@example.org", aDoc1.contacts ().get (0).getEmail ());
    assertEquals ("12345", aDoc1.contacts ().get (0).getPhone ());

    assertEquals ("This is a mock entry for testing purposes only", aDoc1.getAdditionalInformation ());

    // The document type IDs are added to every entity
    assertEquals (1, aDoc1.documentTypeIDs ().size ());

    // Entity 2 - three multilingual names, whose order must be retained
    final PDStoredBusinessEntity aDoc2 = _findByCountry (aDocs, "NO");
    assertNotNull (aDoc2);
    assertEquals (m_aParticipantID, aDoc2.getParticipantID ());
    assertEquals ("junittest", aDoc2.getMetaData ().getOwnerID ());
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

  @Test
  public void testGetAllDocumentsOfCountryCode () throws IOException
  {
    m_aStorageMgr.createOrUpdateEntry (m_aParticipantID,
                                       PDConformanceTestData.createMockBusinessCard (m_aParticipantID),
                                       PDConformanceTestData.createMockMetaData ());

    // No country - no docs
    ICommonsList <PDStoredBusinessEntity> aDocs = m_aStorageMgr.getAllDocuments (PDField.COUNTRY_CODE.getExactMatchQuery (""),
                                                                                 -1);
    assertEquals (0, aDocs.size ());

    // Search for NO
    aDocs = m_aStorageMgr.getAllDocuments (PDField.COUNTRY_CODE.getExactMatchQuery ("NO"), -1);
    assertEquals (1, aDocs.size ());

    final PDStoredBusinessEntity aSingleDoc = aDocs.get (0);
    assertEquals (m_aParticipantID, aSingleDoc.getParticipantID ());
    assertEquals ("junittest", aSingleDoc.getMetaData ().getOwnerID ());
    assertEquals ("NO", aSingleDoc.getCountryCode ());
  }

  @Test
  public void testCreateOrUpdateEntryReplacesTheOldEntities () throws IOException
  {
    final PDStoredMetaData aMetaData = PDConformanceTestData.createMockMetaData ();
    m_aStorageMgr.createOrUpdateEntry (m_aParticipantID,
                                       PDConformanceTestData.createMockBusinessCard (m_aParticipantID),
                                       aMetaData);
    assertEquals (2, m_aStorageMgr.getContainedParticipantCount ());

    // The very same business card must not add any documents
    m_aStorageMgr.createOrUpdateEntry (m_aParticipantID,
                                       PDConformanceTestData.createMockBusinessCard (m_aParticipantID),
                                       aMetaData);
    assertEquals (2, m_aStorageMgr.getContainedParticipantCount ());
    assertEquals (2, m_aStorageMgr.getAllDocumentsOfParticipant (m_aParticipantID).size ());
  }

  @Test
  public void testDeleteEntryWithOwnerVerification () throws IOException
  {
    final PDStoredMetaData aMetaData = PDConformanceTestData.createMockMetaData ();
    m_aStorageMgr.createOrUpdateEntry (m_aParticipantID,
                                       PDConformanceTestData.createMockBusinessCard (m_aParticipantID),
                                       aMetaData);
    assertTrue (m_aStorageMgr.containsEntry (m_aParticipantID));

    // A different owner must not delete anything
    final PDStoredMetaData aOtherOwner = new PDStoredMetaData (PDTFactory.getCurrentLocalDateTime (),
                                                               "someone-else",
                                                               "localhost");
    assertEquals (0, m_aStorageMgr.deleteEntry (m_aParticipantID, aOtherOwner, true));
    assertTrue (m_aStorageMgr.containsEntry (m_aParticipantID));

    // The correct owner deletes all entities of the participant
    assertEquals (2, m_aStorageMgr.deleteEntry (m_aParticipantID, aMetaData, true));
    assertFalse (m_aStorageMgr.containsEntry (m_aParticipantID));
    assertEquals (0, m_aStorageMgr.getContainedParticipantCount ());
  }

  @Test
  public void testGetAllContainedParticipantIDs () throws IOException
  {
    assertTrue (m_aStorageMgr.getAllContainedParticipantIDs ().isEmpty ());

    m_aStorageMgr.createOrUpdateEntry (m_aParticipantID,
                                       PDConformanceTestData.createMockBusinessCard (m_aParticipantID),
                                       PDConformanceTestData.createMockMetaData ());

    // One participant with two business entities
    assertEquals (1, m_aStorageMgr.getAllContainedParticipantIDs ().size ());
    assertEquals (2, m_aStorageMgr.getAllContainedParticipantIDs ().get (m_aParticipantID).intValue ());
  }
}
