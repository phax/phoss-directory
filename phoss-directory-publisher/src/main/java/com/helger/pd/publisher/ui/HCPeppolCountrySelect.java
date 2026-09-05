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

import java.util.Locale;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.helper.CollectionSort;
import com.helger.html.request.IHCRequestField;
import com.helger.peppolid.checks.country.PeppolParticipantCountryHelper;
import com.helger.photon.uicore.html.select.HCExtSelect;
import com.helger.text.compare.ComparatorHelper;
import com.helger.text.locale.country.CountryCache;

/**
 * A select box containing all the countries of the country specific Peppol participant identifier
 * schemes. The first option has an empty value, meaning that no country filter is applied at all,
 * and is the default selection.
 *
 * @author Philip Helger
 */
public class HCPeppolCountrySelect extends HCExtSelect
{
  /** The text of the special option that applies no country filter at all */
  public static final String TEXT_ALL_COUNTRIES = "All countries";

  /**
   * @return A list with the countries of all the country specific Peppol participant identifier
   *         schemes, in no particular order. Never <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  @ReturnsMutableCopy
  public static ICommonsList <Locale> getAllPeppolCountries ()
  {
    final CountryCache aCountryCache = CountryCache.getInstance ();
    final ICommonsList <Locale> ret = new CommonsArrayList <> ();
    // The same country is used by more than one identifier scheme, so the codes must be unified
    for (final String sCountryCode : new CommonsHashSet <> (PeppolParticipantCountryHelper.getAllSchemeCountryCodes ()
                                                                                          .values ()))
    {
      final Locale aCountry = aCountryCache.getCountry (sCountryCode);
      if (aCountry != null)
        ret.add (aCountry);
    }
    return ret;
  }

  public HCPeppolCountrySelect (@NonNull final IHCRequestField aRF, @NonNull final Locale aDisplayLocale)
  {
    super (aRF);

    for (final Locale aCountry : CollectionSort.getSorted (getAllPeppolCountries (),
                                                           ComparatorHelper.getComparatorCollating (x -> x.getDisplayCountry (aDisplayLocale),
                                                                                                    aDisplayLocale)))
      addOption (aCountry.getCountry (), aCountry.getDisplayCountry (aDisplayLocale));

    // An empty value means "no country filter at all"
    addOptionAt (0, createSpecialOption (TEXT_ALL_COUNTRIES));
  }
}
