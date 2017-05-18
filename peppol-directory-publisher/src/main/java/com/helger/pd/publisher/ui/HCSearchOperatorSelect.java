package com.helger.pd.publisher.ui;

import java.util.EnumSet;
import java.util.Locale;

import javax.annotation.Nonnull;

import com.helger.html.request.IHCRequestField;
import com.helger.pd.publisher.search.EPDSearchDataType;
import com.helger.pd.publisher.search.EPDSearchOperator;
import com.helger.photon.uicore.html.select.HCExtSelect;

public class HCSearchOperatorSelect extends HCExtSelect
{
  public HCSearchOperatorSelect (@Nonnull final IHCRequestField aRF,
                                 @Nonnull final EPDSearchDataType eDataType,
                                 @Nonnull final Locale aDisplayLocale)
  {
    this (aRF, eDataType.getAllAllowedOperators (), aDisplayLocale);
  }

  public HCSearchOperatorSelect (@Nonnull final IHCRequestField aRF,
                                 @Nonnull final EnumSet <EPDSearchOperator> aOperators,
                                 @Nonnull final Locale aDisplayLocale)
  {
    super (aRF);

    for (final EPDSearchOperator eOp : aOperators)
      addOption (eOp.getID (), eOp.getDisplayText (aDisplayLocale));
    addOptionPleaseSelect (aDisplayLocale);
  }
}
