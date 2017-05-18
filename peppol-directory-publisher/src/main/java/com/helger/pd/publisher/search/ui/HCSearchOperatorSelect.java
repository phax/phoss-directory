package com.helger.pd.publisher.search.ui;

import java.util.EnumSet;
import java.util.Locale;

import javax.annotation.Nonnull;

import com.helger.html.request.IHCRequestField;
import com.helger.pd.publisher.search.ESearchDataType;
import com.helger.pd.publisher.search.ESearchOperator;
import com.helger.photon.uicore.html.select.HCExtSelect;

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
