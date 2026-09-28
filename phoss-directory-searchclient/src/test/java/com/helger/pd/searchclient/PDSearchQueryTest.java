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
package com.helger.pd.searchclient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.pd.searchapi.CPDSearchAPI;
import com.helger.pd.searchapi.EPDSearchAPIField;
import com.helger.peppolid.factory.PeppolIdentifierFactory;

/**
 * Test class for class {@link PDSearchQuery}.
 *
 * @author Philip Helger
 */
public final class PDSearchQueryTest
{
  @Test
  public void testEmpty ()
  {
    final PDSearchQuery aQuery = new PDSearchQuery ();
    assertFalse (aQuery.hasQueryTerms ());
    assertEquals ("", aQuery.getAsQueryString ());
    assertEquals (CPDSearchAPI.DEFAULT_RESULT_PAGE_INDEX, aQuery.getResultPageIndex ());
    assertEquals (CPDSearchAPI.DEFAULT_RESULT_PAGE_COUNT, aQuery.getResultPageCount ());
  }

  @Test
  public void testQueryString ()
  {
    // The default paging is not part of the query string
    assertEquals ("q=Helger", PDSearchQuery.createGeneric ("Helger").getAsQueryString ());

    // Values are URL encoded
    assertEquals ("name=Philip+Helger+%26+Co", new PDSearchQuery ().name ("Philip Helger & Co").getAsQueryString ());
    assertEquals ("participant=iso6523-actorid-upis%3A%3A9915%3Ahelger",
                  PDSearchQuery.createForParticipant (PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:helger"))
                               .getAsQueryString ());

    // One field may carry multiple values
    assertEquals ("country=AT&country=DE",
                  new PDSearchQuery ().countryCode ("AT").countryCode ("DE").getAsQueryString ());

    // Non-default paging is appended
    assertEquals ("q=Helger&resultPageIndex=2&resultPageCount=50",
                  PDSearchQuery.createGeneric ("Helger").setResultPageIndex (2).setResultPageCount (50)
                               .getAsQueryString ());
  }

  @Test
  public void testParseQueryTerms ()
  {
    // That's the format of the "query-terms" attribute of a result list
    final PDSearchQuery aQuery = PDSearchQuery.parseQueryTerms ("q=Helger&country=AT&country=DE");
    assertTrue (aQuery.hasQueryTerms ());
    assertEquals (2, aQuery.getAllQueryTerms ().size ());
    assertEquals (1, aQuery.getAllQueryTerms ().get (EPDSearchAPIField.GENERIC).size ());
    assertEquals (2, aQuery.getAllQueryTerms ().get (EPDSearchAPIField.COUNTRY).size ());

    // Unknown fields and empty values are ignored
    assertFalse (PDSearchQuery.parseQueryTerms ("bogus=1&q=").hasQueryTerms ());
    assertFalse (PDSearchQuery.parseQueryTerms (null).hasQueryTerms ());
    assertFalse (PDSearchQuery.parseQueryTerms ("").hasQueryTerms ());
  }

  @Test
  public void testBeyondMaxResults ()
  {
    // 50 * 20 = the last result index is 1019 > 1000
    assertTrue (new PDSearchQuery ().setResultPageIndex (50).isBeyondMaxResults ());
    // 49 * 20 = the last result index is 999
    assertFalse (new PDSearchQuery ().setResultPageIndex (49).isBeyondMaxResults ());
    assertFalse (new PDSearchQuery ().isBeyondMaxResults ());
  }

  @Test
  public void testClone ()
  {
    final PDSearchQuery aQuery = PDSearchQuery.createGeneric ("Helger").setResultPageIndex (3);
    final PDSearchQuery aClone = aQuery.getClone ();
    assertEquals (aQuery.getAsQueryString (), aClone.getAsQueryString ());

    // The clone must not share the query term lists with the source
    aClone.countryCode ("AT");
    assertEquals ("q=Helger&resultPageIndex=3", aQuery.getAsQueryString ());
  }
}
