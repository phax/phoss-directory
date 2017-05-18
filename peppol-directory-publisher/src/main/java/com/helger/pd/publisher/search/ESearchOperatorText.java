package com.helger.pd.publisher.search;

import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.text.IMultilingualText;
import com.helger.commons.text.display.IHasDisplayText;
import com.helger.commons.text.resolve.DefaultTextResolver;
import com.helger.commons.text.util.TextHelper;

public enum ESearchOperatorText implements IHasDisplayText
{
  /** Equals */
  EQ ("="),
  /** Not equals */
  NE ("!="),
  /** Lower than */
  LT ("<"),
  /** Lower or equal */
  LE ("<="),
  /** Greater than */
  GT (">"),
  /** Greater or equal */
  GE (">="),
  /** Is empty */
  EMPTY ("leer", "empty"),
  /** Is not empty */
  NOT_EMPTY ("gesetzt", "not empty"),
  /** String contains */
  STRING_CONTAINS ("enthält", "contains"),
  /** String starts with */
  STRING_STARTS_WITH ("beginnt mit", "starts with"),
  /** String ends with */
  STRING_ENDS_WITH ("endet mit", "ends with"),
  /** String matches regular expression */
  STRING_REGEX ("entspricht regulärem Ausdruck", "matches regular expression"),
  /** Int even */
  INT_EVEN ("gerade", "even"),
  /** Int odd */
  INT_ODD ("ungerade", "odd"),
  /** Year of date */
  DATE_YEAR ("Jahr", "year"),
  /** Month of date */
  DATE_MONTH ("Monat", "month"),
  /** Day of date */
  DATE_DAY ("Tag", "day"),
  /** Year and month of date */
  DATE_YEAR_MONTH ("Jahr und Monat", "year and month"),
  /** Month and day of date */
  DATE_MONTH_DAY ("Monat und Tag", "month and day"),
  /** Hour of time */
  TIME_HOUR ("Stunde", "hour"),
  /** Minute of time */
  TIME_MINUTE ("Minute", "minute"),
  /** Second of time */
  TIME_SECOND ("Sekunde", "second");

  private final IMultilingualText m_aTP;

  private ESearchOperatorText (@Nonnull final String sGeneric)
  {
    this (sGeneric, sGeneric);
  }

  private ESearchOperatorText (@Nonnull final String sDE, @Nonnull final String sEN)
  {
    m_aTP = TextHelper.create_DE_EN (sDE, sEN);
  }

  @Nullable
  public String getDisplayText (@Nonnull final Locale aContentLocale)
  {
    return DefaultTextResolver.getTextStatic (this, m_aTP, aContentLocale);
  }
}
