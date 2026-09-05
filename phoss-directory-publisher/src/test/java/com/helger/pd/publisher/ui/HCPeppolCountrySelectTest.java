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
package com.helger.pd.publisher.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.junit.Rule;
import org.junit.Test;

import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSet;
import com.helger.html.hc.html.forms.HCOption;
import com.helger.pd.publisher.PDPublisherTestRule;
import com.helger.peppolid.checks.country.PeppolParticipantCountryHelper;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.uicore.html.select.AbstractHCExtSelect;

/**
 * Test class for class {@link HCPeppolCountrySelect}.
 *
 * @author Philip Helger
 */
public final class HCPeppolCountrySelectTest
{
  @Rule
  public final PDPublisherTestRule m_aRule = new PDPublisherTestRule ();

  @Test
  public void testGetAllPeppolCountries ()
  {
    final ICommonsList <Locale> aCountries = HCPeppolCountrySelect.getAllPeppolCountries ();
    assertTrue (aCountries.isNotEmpty ());

    // Every country of the helper must be resolvable and must be contained exactly once
    final ICommonsSet <String> aExpected = new CommonsHashSet <> (PeppolParticipantCountryHelper.getAllSchemeCountryCodes ()
                                                                                                .values ());
    assertEquals (aExpected.size (), aCountries.size ());
    assertEquals (aExpected, new CommonsHashSet <> (aCountries, Locale::getCountry));
  }

  @Test
  public void testSelect ()
  {
    final HCPeppolCountrySelect aSelect = new HCPeppolCountrySelect (new RequestField ("country"), Locale.UK);
    final ICommonsList <HCOption> aOptions = aSelect.getAllOptions ();

    // All countries plus the "all countries" option
    assertEquals (HCPeppolCountrySelect.getAllPeppolCountries ().size () + 1, aOptions.size ());

    // The default option applies no filter at all
    final HCOption aFirst = aOptions.getFirstOrNull ();
    assertEquals (AbstractHCExtSelect.VALUE_PLEASE_SELECT, aFirst.getValue ());
    assertTrue (aFirst.containsClass (AbstractHCExtSelect.CSS_CLASS_SPECIAL_OPTION));

    // All other options are ISO 3166-1 alpha-2 country codes
    for (final HCOption aOption : aOptions.subList (1, aOptions.size ()))
      assertEquals (2, aOption.getValue ().length ());
  }
}
