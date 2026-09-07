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
package com.helger.pd.indexer.storage;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.helger.pd.indexer.searchindex.query.EPDIndexQueryOccur;
import com.helger.pd.indexer.searchindex.query.IPDIndexQuery;
import com.helger.pd.indexer.searchindex.query.PDIndexQueryBool;
import com.helger.pd.indexer.storage.field.PDField;

/**
 * Test class for class {@link PDQueryManager}.
 *
 * @author Philip Helger
 */
public final class PDQueryManagerTest
{
  private static IPDIndexQuery _createSynonymQuery (final String... aCountryCodes)
  {
    final PDIndexQueryBool.Builder aBuilder = new PDIndexQueryBool.Builder ();
    for (final String sCountryCode : aCountryCodes)
      aBuilder.add (PDField.COUNTRY_CODE.getExactMatchQuery (sCountryCode), EPDIndexQueryOccur.SHOULD);
    return aBuilder.build ();
  }

  @Test
  public void testGetCountryCodeQueryWithoutSynonyms ()
  {
    final IPDIndexQuery aExpected = PDField.COUNTRY_CODE.getExactMatchQuery ("AT");
    assertEquals (aExpected, PDQueryManager.getCountryCodeQuery ("AT"));
    // The country code is upper cased
    assertEquals (aExpected, PDQueryManager.getCountryCodeQuery ("at"));
    assertEquals (aExpected, PDQueryManager.getCountryCodeQuery ("At"));
  }

  @Test
  public void testGetCountryCodeQueryUnitedKingdom ()
  {
    // A Business Card may use "GB" as well as "UK" - both must find the same entries
    final IPDIndexQuery aExpected = _createSynonymQuery ("GB", "UK");
    assertEquals (aExpected, PDQueryManager.getCountryCodeQuery ("GB"));
    assertEquals (aExpected, PDQueryManager.getCountryCodeQuery ("UK"));
    assertEquals (aExpected, PDQueryManager.getCountryCodeQuery ("gb"));
    assertEquals (aExpected, PDQueryManager.getCountryCodeQuery ("uk"));
  }

  @Test
  public void testGetCountryCodeQueryGreece ()
  {
    // A Business Card may use "GR" as well as "EL" - both must find the same entries
    final IPDIndexQuery aExpected = _createSynonymQuery ("GR", "EL");
    assertEquals (aExpected, PDQueryManager.getCountryCodeQuery ("GR"));
    assertEquals (aExpected, PDQueryManager.getCountryCodeQuery ("EL"));
    assertEquals (aExpected, PDQueryManager.getCountryCodeQuery ("gr"));
    assertEquals (aExpected, PDQueryManager.getCountryCodeQuery ("el"));
  }
}
