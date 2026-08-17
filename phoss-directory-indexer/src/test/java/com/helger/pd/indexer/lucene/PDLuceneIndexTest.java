/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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
package com.helger.pd.indexer.lucene;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.io.IOException;

import org.jspecify.annotations.NonNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.pd.indexer.PDIndexerTestRule;
import com.helger.pd.indexer.searchindex.PDIndexDocument;
import com.helger.pd.indexer.searchindex.query.EPDIndexQueryOccur;
import com.helger.pd.indexer.searchindex.query.IPDIndexQuery;
import com.helger.pd.indexer.searchindex.query.PDIndexQueryBool;
import com.helger.pd.indexer.searchindex.query.PDIndexQueryContains;
import com.helger.pd.indexer.storage.field.PDField;

/**
 * Test class for class {@link PDLuceneIndex}.
 *
 * @author Philip Helger
 */
public final class PDLuceneIndexTest
{
  @Rule
  public final TestRule m_aRule = new PDIndexerTestRule ();

  private static void _addMockDocument (@NonNull final PDLuceneIndex aIndex) throws IOException
  {
    final PDIndexDocument aDoc = new PDIndexDocument ();
    aDoc.add (PDField.COUNTRY_CODE.getAsField ("AT"));
    aDoc.add (PDField.NAME.getAsField ("Test entry"));
    aIndex.updateDocuments (null, new CommonsArrayList <> (aDoc));
  }

  @Test
  public void testNativeQueryIsCached () throws IOException
  {
    try (final PDLuceneIndex aIndex = new PDLuceneIndex ())
    {
      _addMockDocument (aIndex);

      final IPDIndexQuery aQuery = PDField.COUNTRY_CODE.getExactMatchQuery ("AT");
      assertNull (aQuery.getNativeQuery ());

      assertEquals (1, aIndex.getCount (aQuery));
      final Object aNativeQuery = aQuery.getNativeQuery ();
      assertNotNull (aNativeQuery);

      // Executing the same query object again must not translate it again
      assertEquals (1, aIndex.getCount (aQuery));
      assertSame (aNativeQuery, aQuery.getNativeQuery ());
    }
  }

  @Test
  public void testNativeQueryIsCachedRecursively () throws IOException
  {
    try (final PDLuceneIndex aIndex = new PDLuceneIndex ())
    {
      _addMockDocument (aIndex);

      // Creating a "contains" query requires the compilation of an automaton
      final IPDIndexQuery aInnerQuery = new PDIndexQueryContains (PDField.NAME.getFieldName (), "test");
      final IPDIndexQuery aQuery = new PDIndexQueryBool.Builder ().add (aInnerQuery, EPDIndexQueryOccur.FILTER)
                                                                  .build ();
      assertNull (aQuery.getNativeQuery ());
      assertNull (aInnerQuery.getNativeQuery ());

      assertEquals (1, aIndex.getCount (aQuery));
      assertNotNull (aQuery.getNativeQuery ());

      // The nested queries must be cached as well
      final Object aNativeInnerQuery = aInnerQuery.getNativeQuery ();
      assertNotNull (aNativeInnerQuery);

      assertEquals (1, aIndex.getCount (aQuery));
      assertSame (aNativeInnerQuery, aInnerQuery.getNativeQuery ());
    }
  }
}
