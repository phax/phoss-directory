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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.helger.base.io.stream.StreamHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.pd.indexer.searchindex.IPDIndex;
import com.helger.pd.indexer.searchindex.PDIndexDocument;
import com.helger.pd.indexer.searchindex.query.EPDIndexQueryOccur;
import com.helger.pd.indexer.searchindex.query.IPDIndexQuery;
import com.helger.pd.indexer.searchindex.query.PDIndexQueryBool;
import com.helger.pd.indexer.searchindex.query.PDIndexQueryContains;
import com.helger.pd.indexer.searchindex.query.PDIndexQueryMatchAll;
import com.helger.pd.indexer.searchindex.query.PDIndexQueryTerm;
import com.helger.pd.indexer.storage.CPDStorage;
import com.helger.pd.indexer.storage.PDStoredBusinessEntity;
import com.helger.pd.indexer.storage.field.PDField;
import com.helger.peppolid.IParticipantIdentifier;

/**
 * The conformance test suite that every implementation of {@link IPDIndex} must pass. Derive from
 * this class, implement {@link #createIndex()} and add the {@link org.junit.Rule} that the
 * implementation needs.<br>
 * Only behaviour that all implementations must agree on is asserted here. Especially the order in
 * which {@link IPDIndex#searchAll(IPDIndexQuery, int, java.util.function.Consumer)} returns the
 * matching documents is <b>not</b> part of the contract.
 *
 * @author Philip Helger
 * @since 0.16.0
 */
public abstract class AbstractPDIndexConformanceTest
{
  private IPDIndex m_aIndex;
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
   * @return The search index under test. Never <code>null</code> inside a test method.
   */
  @NonNull
  protected final IPDIndex index ()
  {
    return m_aIndex;
  }

  /**
   * @return The participant ID all test documents belong to. Never <code>null</code> inside a test
   *         method.
   */
  @NonNull
  protected final IParticipantIdentifier participantID ()
  {
    return m_aParticipantID;
  }

  @Before
  public final void beforeIndexConformanceTest () throws IOException
  {
    m_aParticipantID = PDConformanceTestData.createParticipantID ();
    assertNotNull (m_aParticipantID);

    m_aIndex = createIndex ();
    assertNotNull (m_aIndex);

    // Every test starts with an empty index
    m_aIndex.deleteDocuments (PDIndexQueryMatchAll.INSTANCE);
    assertEquals (0, m_aIndex.getCount (PDIndexQueryMatchAll.INSTANCE));
  }

  @After
  public final void afterIndexConformanceTest () throws IOException
  {
    if (m_aIndex != null)
    {
      // Don't leave anything behind for the next test class
      if (!m_aIndex.isClosing ())
        m_aIndex.deleteDocuments (PDIndexQueryMatchAll.INSTANCE);
      StreamHelper.close (m_aIndex);
      m_aIndex = null;
    }
  }

  /**
   * Add the two mock documents of {@link #participantID()} to the index.
   *
   * @throws IOException
   *         On index error
   */
  protected final void addMockDocuments () throws IOException
  {
    m_aIndex.updateDocuments (null, PDConformanceTestData.createMockIndexDocuments (m_aParticipantID));
  }

  @NonNull
  private ICommonsList <PDIndexDocument> _searchAll (@NonNull final IPDIndexQuery aQuery,
                                                     final int nMaxResultCount) throws IOException
  {
    final ICommonsList <PDIndexDocument> ret = new CommonsArrayList <> ();
    m_aIndex.searchAll (aQuery, nMaxResultCount, ret::add);
    return ret;
  }

  @Test
  public void testEmptyIndex () throws IOException
  {
    assertEquals (0, m_aIndex.getCount (PDIndexQueryMatchAll.INSTANCE));
    assertEquals (0, m_aIndex.getCount (PDField.PARTICIPANT_ID.getExactMatchQuery (m_aParticipantID)));
    assertTrue (_searchAll (PDIndexQueryMatchAll.INSTANCE, -1).isEmpty ());
  }

  @Test
  public void testAddAndReadBackAllFieldTypes () throws IOException
  {
    addMockDocuments ();

    final IPDIndexQuery aQuery = PDField.PARTICIPANT_ID.getExactMatchQuery (m_aParticipantID);
    assertEquals (2, m_aIndex.getCount (aQuery));

    final ICommonsList <PDIndexDocument> aDocs = _searchAll (aQuery, -1);
    assertEquals (2, aDocs.size ());

    for (final PDIndexDocument aDoc : aDocs)
    {
      final PDStoredBusinessEntity aEntity = PDStoredBusinessEntity.create (aDoc);

      // Not tokenized String field
      assertEquals (m_aParticipantID, aEntity.getParticipantID ());
      assertEquals ("AT", aEntity.getCountryCode ());
      assertEquals (1, aEntity.documentTypeIDs ().size ());
      assertNotNull (aEntity.getRegistrationDate ());

      // Tokenized String fields are stored unchanged
      assertEquals ("Vienna Austria", aEntity.getGeoInfo ());
      assertEquals ("Some additional information", aEntity.getAdditionalInformation ());
      assertEquals (1, aEntity.names ().size ());
      assertTrue (aEntity.names ().getFirstOrNull ().getName ().startsWith ("Test Company "));
      assertNotNull (aEntity.names ().getFirstOrNull ().getLanguageCode ());

      // Parallel multi value fields keep their order
      assertEquals (1, aEntity.identifiers ().size ());
      assertEquals ("vat", aEntity.identifiers ().getFirstOrNull ().getScheme ());
      assertEquals ("atu12345678", aEntity.identifiers ().getFirstOrNull ().getValue ());
      assertEquals (1, aEntity.websiteURIs ().size ());
      assertEquals ("https://www.example.org", aEntity.websiteURIs ().getFirstOrNull ());
      assertEquals (1, aEntity.contacts ().size ());
      assertEquals ("support", aEntity.contacts ().getFirstOrNull ().getType ());
      assertEquals ("john doe", aEntity.contacts ().getFirstOrNull ().getName ());
      assertEquals ("+43 1 234567", aEntity.contacts ().getFirstOrNull ().getPhone ());
      assertEquals ("support@example.org", aEntity.contacts ().getFirstOrNull ().getEmail ());

      // Numeric field
      assertNotNull (aEntity.getMetaData ().getCreationDT ());
      assertEquals (PDConformanceTestData.OWNER_ID, aEntity.getMetaData ().getOwnerID ());
      assertEquals ("127.0.0.1", aEntity.getMetaData ().getRequestingHost ());
    }
  }

  @Test
  public void testFieldThatIsNotStoredIsNotReadBack () throws IOException
  {
    addMockDocuments ();

    // The "all fields" field is indexed, so it must be searchable
    assertEquals (2, m_aIndex.getCount (new PDIndexQueryContains (CPDStorage.FIELD_ALL_FIELDS, "ienn")));

    // ... but it is not stored, so it must not be part of a result document
    for (final PDIndexDocument aDoc : _searchAll (PDIndexQueryMatchAll.INSTANCE, -1))
      assertNull (aDoc.getFieldOfName (CPDStorage.FIELD_ALL_FIELDS));
  }

  @Test
  public void testMaxResultCount () throws IOException
  {
    addMockDocuments ();

    assertEquals (1, _searchAll (PDIndexQueryMatchAll.INSTANCE, 1).size ());
    assertEquals (2, _searchAll (PDIndexQueryMatchAll.INSTANCE, 2).size ());
    // More than available
    assertEquals (2, _searchAll (PDIndexQueryMatchAll.INSTANCE, 100).size ());
    // All
    assertEquals (2, _searchAll (PDIndexQueryMatchAll.INSTANCE, -1).size ());
    assertEquals (2, _searchAll (PDIndexQueryMatchAll.INSTANCE, 0).size ());
  }

  @Test
  public void testUpdateDocumentsDeletesTheOldOnes () throws IOException
  {
    addMockDocuments ();
    assertEquals (2, m_aIndex.getCount (PDIndexQueryMatchAll.INSTANCE));

    // Now the participant has a single business entity only
    m_aIndex.updateDocuments (PDField.PARTICIPANT_ID.getExactMatchQuery (m_aParticipantID),
                              new CommonsArrayList <> (PDConformanceTestData.createMockIndexDocument (m_aParticipantID,
                                                                                                      "Test Company GmbH",
                                                                                                      "de")));
    assertEquals (1, m_aIndex.getCount (PDIndexQueryMatchAll.INSTANCE));
    assertEquals (1, m_aIndex.getCount (PDField.ML_LANGUAGE.getExactMatchQuery ("de")));
    assertEquals (0, m_aIndex.getCount (PDField.ML_LANGUAGE.getExactMatchQuery ("en")));
  }

  @Test
  public void testUpdateDocumentsWithoutDeleteQueryAdds () throws IOException
  {
    addMockDocuments ();
    addMockDocuments ();
    assertEquals (4, m_aIndex.getCount (PDIndexQueryMatchAll.INSTANCE));
  }

  @Test
  public void testDeleteDocuments () throws IOException
  {
    addMockDocuments ();

    m_aIndex.deleteDocuments (PDField.ML_LANGUAGE.getExactMatchQuery ("de"));
    assertEquals (1, m_aIndex.getCount (PDIndexQueryMatchAll.INSTANCE));

    m_aIndex.deleteDocuments (PDField.PARTICIPANT_ID.getExactMatchQuery (m_aParticipantID));
    assertEquals (0, m_aIndex.getCount (PDIndexQueryMatchAll.INSTANCE));
  }

  @Test
  public void testExactMatchQuery () throws IOException
  {
    addMockDocuments ();

    // Not tokenized field - the whole value must match
    assertEquals (2, m_aIndex.getCount (PDField.COUNTRY_CODE.getExactMatchQuery ("AT")));
    assertEquals (0, m_aIndex.getCount (PDField.COUNTRY_CODE.getExactMatchQuery ("at")));
    assertEquals (0, m_aIndex.getCount (PDField.COUNTRY_CODE.getExactMatchQuery ("NO")));

    // Tokenized field - a single term matches
    assertEquals (2, m_aIndex.getCount (new PDIndexQueryTerm (PDField.ML_NAME.getFieldName (), "company")));
    // ... and the terms are lower cased when indexed
    assertEquals (0, m_aIndex.getCount (new PDIndexQueryTerm (PDField.ML_NAME.getFieldName (), "Company")));
  }

  @Test
  public void testPrefixQuery () throws IOException
  {
    addMockDocuments ();

    // The stored owner ID is longer than the queried one
    assertEquals (2, m_aIndex.getCount (PDField.METADATA_OWNERID.getPrefixQuery (PDConformanceTestData.OWNER_ID_PREFIX)));
    assertEquals (2, m_aIndex.getCount (PDField.METADATA_OWNERID.getPrefixQuery (PDConformanceTestData.OWNER_ID)));
    assertEquals (0, m_aIndex.getCount (PDField.METADATA_OWNERID.getPrefixQuery ("CN=other")));
  }

  @Test
  public void testContainsQuery () throws IOException
  {
    addMockDocuments ();

    // "contains" on a tokenized field works on the single terms
    assertEquals (2, m_aIndex.getCount (new PDIndexQueryContains (PDField.ML_NAME.getFieldName (), "ompan")));
    assertEquals (1, m_aIndex.getCount (new PDIndexQueryContains (PDField.ML_NAME.getFieldName (), "mb")));
    assertEquals (0, m_aIndex.getCount (new PDIndexQueryContains (PDField.ML_NAME.getFieldName (), "xyz")));
  }

  @Test
  public void testBoolQuery () throws IOException
  {
    addMockDocuments ();

    // All clauses must match
    assertEquals (1,
                  m_aIndex.getCount (new PDIndexQueryBool.Builder ().add (PDField.PARTICIPANT_ID.getExactMatchQuery (m_aParticipantID),
                                                                          EPDIndexQueryOccur.FILTER)
                                                                    .add (PDField.ML_LANGUAGE.getExactMatchQuery ("de"),
                                                                          EPDIndexQueryOccur.FILTER)
                                                                    .build ()));
    assertEquals (1,
                  m_aIndex.getCount (new PDIndexQueryBool.Builder ().add (PDField.PARTICIPANT_ID.getExactMatchQuery (m_aParticipantID),
                                                                          EPDIndexQueryOccur.MUST)
                                                                    .add (PDField.ML_LANGUAGE.getExactMatchQuery ("en"),
                                                                          EPDIndexQueryOccur.MUST)
                                                                    .build ()));

    // At least one clause must match
    assertEquals (2,
                  m_aIndex.getCount (new PDIndexQueryBool.Builder ().add (PDField.ML_LANGUAGE.getExactMatchQuery ("de"),
                                                                          EPDIndexQueryOccur.SHOULD)
                                                                    .add (PDField.ML_LANGUAGE.getExactMatchQuery ("en"),
                                                                          EPDIndexQueryOccur.SHOULD)
                                                                    .build ()));

    // Contradicting clauses match nothing
    assertEquals (0,
                  m_aIndex.getCount (new PDIndexQueryBool.Builder ().add (PDField.ML_LANGUAGE.getExactMatchQuery ("de"),
                                                                          EPDIndexQueryOccur.MUST)
                                                                    .add (PDField.ML_LANGUAGE.getExactMatchQuery ("en"),
                                                                          EPDIndexQueryOccur.MUST)
                                                                    .build ()));

    // Nested boolean queries
    final IPDIndexQuery aNested = new PDIndexQueryBool.Builder ().add (PDField.ML_LANGUAGE.getExactMatchQuery ("de"),
                                                                       EPDIndexQueryOccur.SHOULD)
                                                                 .add (PDField.ML_LANGUAGE.getExactMatchQuery ("en"),
                                                                       EPDIndexQueryOccur.SHOULD)
                                                                 .build ();
    assertEquals (2,
                  m_aIndex.getCount (new PDIndexQueryBool.Builder ().add (PDField.COUNTRY_CODE.getExactMatchQuery ("AT"),
                                                                          EPDIndexQueryOccur.FILTER)
                                                                    .add (aNested, EPDIndexQueryOccur.FILTER)
                                                                    .build ()));
  }

  @Test
  public void testGetSplitIntoTerms () throws IOException
  {
    // The analyzer of a tokenized field lower cases and drops the punctuation
    assertEquals (new CommonsArrayList <> ("test", "company", "gmbh"),
                  m_aIndex.getSplitIntoTerms (PDField.ML_NAME.getFieldName (), "Test: Company GmbH"));
    assertEquals (new CommonsArrayList <> ("vienna"),
                  m_aIndex.getSplitIntoTerms (PDField.GEO_INFO.getFieldName (), "Vienna"));
    assertEquals (new CommonsArrayList <> ("9915", "testcompany"),
                  m_aIndex.getSplitIntoTerms (CPDStorage.FIELD_ALL_FIELDS, "9915:testcompany"));
  }

  @Test
  public void testNativeQueryIsCachedRecursively () throws IOException
  {
    addMockDocuments ();

    final IPDIndexQuery aInnerQuery = new PDIndexQueryContains (PDField.ML_NAME.getFieldName (), "ompan");
    final IPDIndexQuery aQuery = new PDIndexQueryBool.Builder ().add (aInnerQuery, EPDIndexQueryOccur.FILTER).build ();
    assertNull (aQuery.getNativeQuery ());
    assertNull (aInnerQuery.getNativeQuery ());

    assertEquals (2, m_aIndex.getCount (aQuery));
    final Object aNativeQuery = aQuery.getNativeQuery ();
    assertNotNull (aNativeQuery);

    // The nested queries must be cached as well
    final Object aNativeInnerQuery = aInnerQuery.getNativeQuery ();
    assertNotNull (aNativeInnerQuery);

    // Executing the same query object again must not translate it again
    assertEquals (2, m_aIndex.getCount (aQuery));
    assertSame (aNativeQuery, aQuery.getNativeQuery ());
    assertSame (aNativeInnerQuery, aInnerQuery.getNativeQuery ());
  }

  @Test
  public void testGetIndexInformation () throws IOException
  {
    assertTrue (m_aIndex.getIndexInformation ().isNotEmpty ());
  }

  @Test
  public void testIsClosing () throws IOException
  {
    assertTrue (!m_aIndex.isClosing ());
    m_aIndex.close ();
    assertTrue (m_aIndex.isClosing ());
  }
}
