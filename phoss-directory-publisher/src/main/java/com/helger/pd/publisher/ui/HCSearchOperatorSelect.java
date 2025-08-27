/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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

import java.util.EnumSet;
import java.util.Locale;

import com.helger.html.request.IHCRequestField;
import com.helger.pd.publisher.search.ESearchDataType;
import com.helger.pd.publisher.search.ESearchOperator;
import com.helger.photon.uicore.html.select.HCExtSelect;

import jakarta.annotation.Nonnull;

public class HCSearchOperatorSelect extends HCExtSelect
{
  public HCSearchOperatorSelect (@Nonnull final IHCRequestField aRF,
                                 @Nonnull final ESearchDataType eDataType,
                                 @Nonnull final Locale aDisplayLocale)
  {
    this (aRF, eDataType.getAllAllowedOperators (), aDisplayLocale);
  }

  public HCSearchOperatorSelect (@Nonnull final IHCRequestField aRF,
                                 @Nonnull final EnumSet <ESearchOperator> aOperators,
                                 @Nonnull final Locale aDisplayLocale)
  {
    super (aRF);

    for (final ESearchOperator eOp : aOperators)
      addOption (eOp.getID (), eOp.getDisplayText (aDisplayLocale));
    addOptionPleaseSelect (aDisplayLocale);
  }
}
