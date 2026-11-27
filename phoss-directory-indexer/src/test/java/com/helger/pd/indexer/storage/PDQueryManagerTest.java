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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.pd.indexer.searchindex.query.EPDIndexQueryOccur;
import com.helger.pd.indexer.searchindex.query.IPDIndexQuery;
import com.helger.pd.indexer.searchindex.query.PDIndexQueryBool;
import com.helger.pd.indexer.storage.field.PDField;
import com.helger.peppolid.factory.PeppolIdentifierFactory;

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

  private static IPDIndexQuery _createParticipantIDQuery (final String sICD, final String sValue)
  {
    final PDIndexQueryBool.Builder aIdentifierBuilder = new PDIndexQueryBool.Builder ();
    aIdentifierBuilder.add (PDField.IDENTIFIER_SCHEME.getExactMatchQuery (sICD), EPDIndexQueryOccur.FILTER);
    aIdentifierBuilder.add (PDField.IDENTIFIER_VALUE.getExactMatchQuery (sValue), EPDIndexQueryOccur.FILTER);

    final PDIndexQueryBool.Builder aBuilder = new PDIndexQueryBool.Builder ();
    aBuilder.add (PDField.PARTICIPANT_ID.getExactMatchQuery (PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme (sICD +
                                                                                                                                         ':' +
                                                                                                                                         sValue)),
                  EPDIndexQueryOccur.SHOULD);
    aBuilder.add (aIdentifierBuilder.build (), EPDIndexQueryOccur.SHOULD);
    return aBuilder.build ();
  }

  @Test
  public void testGetParticipantIDGenericQuery ()
  {
    final PeppolIdentifierFactory aIF = PeppolIdentifierFactory.INSTANCE;
    final IPDIndexQuery aExpected = _createParticipantIDQuery ("0208", "0123456789");

    assertEquals (aExpected, PDQueryManager.getParticipantIDGenericQuery (aIF, "0208:0123456789"));
    assertEquals (aExpected, PDQueryManager.getParticipantIDGenericQuery (aIF, "  0208:0123456789 "));
    assertEquals (aExpected,
                  PDQueryManager.getParticipantIDGenericQuery (aIF, "iso6523-actorid-upis::0208:0123456789"));
    assertEquals (aExpected,
                  PDQueryManager.getParticipantIDGenericQuery (aIF, "ISO6523-ACTORID-UPIS::0208:0123456789"));

    // Participant identifiers are case insensitive
    assertEquals (_createParticipantIDQuery ("9925", "be0123456789"),
                  PDQueryManager.getParticipantIDGenericQuery (aIF, "9925:BE0123456789"));

    // All the characters POLICY 1 allows
    assertEquals (_createParticipantIDQuery ("0088", "az-09._~"),
                  PDQueryManager.getParticipantIDGenericQuery (aIF, "0088:aZ-09._~"));

    // Identifier of 1 and of 130 characters
    assertEquals (_createParticipantIDQuery ("9915", "a"), PDQueryManager.getParticipantIDGenericQuery (aIF, "9915:a"));
    final String s130 = "a".repeat (130);
    assertEquals (_createParticipantIDQuery ("9915", s130),
                  PDQueryManager.getParticipantIDGenericQuery (aIF, "9915:" + s130));
  }

  @Test
  public void testRegExParticipantID ()
  {
    assertTrue ("0208:0123456789".matches (PDQueryManager.REGEX_PARTICIPANT_ID));
    assertTrue ("iso6523-actorid-upis::0208:0123456789".matches (PDQueryManager.REGEX_PARTICIPANT_ID));
    assertTrue ("Iso6523-ActorId-Upis::0088:aZ-09._~".matches (PDQueryManager.REGEX_PARTICIPANT_ID));

    assertFalse ("0208:".matches (PDQueryManager.REGEX_PARTICIPANT_ID));
    assertFalse ("0208:0123456789+".matches (PDQueryManager.REGEX_PARTICIPANT_ID));
    assertFalse ("busdox-actorid-upis::0208:0123456789".matches (PDQueryManager.REGEX_PARTICIPANT_ID));
    assertFalse (("9915:" + "a".repeat (131)).matches (PDQueryManager.REGEX_PARTICIPANT_ID));
  }

  @Test
  public void testGetParticipantIDGenericQueryNoParticipantID ()
  {
    final PeppolIdentifierFactory aIF = PeppolIdentifierFactory.INSTANCE;

    // Regular search terms
    assertNull (PDQueryManager.getParticipantIDGenericQuery (aIF, "acme"));
    assertNull (PDQueryManager.getParticipantIDGenericQuery (aIF, "0123456789"));
    assertNull (PDQueryManager.getParticipantIDGenericQuery (aIF, "BE0123456789"));
    assertNull (PDQueryManager.getParticipantIDGenericQuery (aIF, "acme 0208:0123456789"));
    assertNull (PDQueryManager.getParticipantIDGenericQuery (aIF, "0208:0123 456789"));

    // Incomplete or malformed identifiers
    assertNull (PDQueryManager.getParticipantIDGenericQuery (aIF, "0208"));
    assertNull (PDQueryManager.getParticipantIDGenericQuery (aIF, "0208:"));
    assertNull (PDQueryManager.getParticipantIDGenericQuery (aIF, "208:0123456789"));
    assertNull (PDQueryManager.getParticipantIDGenericQuery (aIF, "02080:0123456789"));

    // Characters POLICY 1 does not allow
    assertNull (PDQueryManager.getParticipantIDGenericQuery (aIF, "0208:0123456789+"));
    assertNull (PDQueryManager.getParticipantIDGenericQuery (aIF, "0208:0123/456789"));
    assertNull (PDQueryManager.getParticipantIDGenericQuery (aIF, "0208:0123:456789"));
    assertNull (PDQueryManager.getParticipantIDGenericQuery (aIF, "0208:0123*"));
    assertNull (PDQueryManager.getParticipantIDGenericQuery (aIF, "0208:münchen"));

    // Identifier longer than 130 characters
    assertNull (PDQueryManager.getParticipantIDGenericQuery (aIF, "9915:" + "a".repeat (131)));

    // Other or unknown identifier schemes
    assertNull (PDQueryManager.getParticipantIDGenericQuery (aIF, "foo::0208:0123456789"));
    assertNull (PDQueryManager.getParticipantIDGenericQuery (aIF, "busdox-actorid-upis::0208:0123456789"));
    assertNull (PDQueryManager.getParticipantIDGenericQuery (aIF, "iso6523-actorid-upis:0208:0123456789"));
  }
}
