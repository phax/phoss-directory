/*
 * Copyright (C) 2015-2023 Philip Helger (www.helger.com)
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
package com.helger.pd.publisher.search;

import java.time.MonthDay;
import java.time.YearMonth;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;
import com.helger.commons.lang.EnumHelper;
import com.helger.commons.text.display.IHasDisplayText;

public enum ESearchOperator implements IHasDisplayText, IHasID <String>
{
  /** Equals */
  EQ ("eq", true, null, ESearchOperatorText.EQ),
  /** Not equals */
  NE ("ne", true, null, ESearchOperatorText.NE),
  /** Lower than */
  LT ("lt", true, null, ESearchOperatorText.LT),
  /** Lower or equal */
  LE ("le", true, null, ESearchOperatorText.LE),
  /** Greater than */
  GT ("gt", true, null, ESearchOperatorText.GT),
  /** Greater or equal */
  GE ("ge", true, null, ESearchOperatorText.GE),
  /** Is empty */
  EMPTY ("empty", false, null, ESearchOperatorText.EMPTY),
  /** Is not empty */
  NOT_EMPTY ("notempty", false, null, ESearchOperatorText.NOT_EMPTY),
  /** String contains */
  STRING_CONTAINS ("contains", true, String.class, ESearchOperatorText.STRING_CONTAINS),
  /** String starts with */
  STRING_STARTS_WITH ("startswith", true, String.class, ESearchOperatorText.STRING_STARTS_WITH),
  /** String ends with */
  STRING_ENDS_WITH ("endswith", true, String.class, ESearchOperatorText.STRING_ENDS_WITH),
  /** String matches regular expression */
  STRING_REGEX ("regex", true, String.class, ESearchOperatorText.STRING_REGEX),
  /** Int even */
  INT_EVEN ("even", false, null, ESearchOperatorText.INT_EVEN),
  /** Int odd */
  INT_ODD ("odd", false, null, ESearchOperatorText.INT_ODD),
  /** Year of date */
  DATE_YEAR ("year", true, int.class, ESearchOperatorText.DATE_YEAR),
  /** Month of date */
  DATE_MONTH ("month", true, int.class, ESearchOperatorText.DATE_MONTH),
  /** Day of date */
  DATE_DAY ("day", true, int.class, ESearchOperatorText.DATE_DAY),
  /** Year and month of date */
  DATE_YEAR_MONTH ("yearmonth", true, YearMonth.class, ESearchOperatorText.DATE_YEAR_MONTH),
  /** Month and day of date */
  DATE_MONTH_DAY ("monthday", true, MonthDay.class, ESearchOperatorText.DATE_MONTH_DAY),
  /** Hour of time */
  TIME_HOUR ("hour", true, int.class, ESearchOperatorText.TIME_HOUR),
  /** Minute of time */
  TIME_MINUTE ("minute", true, int.class, ESearchOperatorText.TIME_MINUTE),
  /** Second of time */
  TIME_SECOND ("second", true, int.class, ESearchOperatorText.TIME_SECOND);

  private final String m_sID;
  private final boolean m_bNeedsValue;
  private final Class <?> m_aSpecialValueClass;
  private final ESearchOperatorText m_eDisplayText;

  private ESearchOperator (@Nonnull @Nonempty final String sID,
                           final boolean bNeedsValue,
                           @Nullable final Class <?> aSpecialValueClass,
                           @Nonnull final ESearchOperatorText eDisplayText)
  {
    m_sID = sID;
    m_bNeedsValue = bNeedsValue;
    m_aSpecialValueClass = aSpecialValueClass;
    m_eDisplayText = eDisplayText;
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  /**
   * @return <code>true</code> if a search value is needed (mostly),
   *         <code>false</code> if not (e.g. for 'empty')
   */
  public boolean isValueNeeded ()
  {
    return m_bNeedsValue;
  }

  /**
   * @return The special value classes needed for search values or
   *         <code>null</code> if the value should have the same class as the
   *         reference value.
   */
  @Nullable
  public Class <?> getSpecialValueClass ()
  {
    return m_aSpecialValueClass;
  }

  /**
   * @return <code>true</code> if search values of this operator need to have a
   *         special class!
   */
  public boolean hasSpecialValueClass ()
  {
    return m_aSpecialValueClass != null;
  }

  @Nullable
  public String getDisplayText (@Nonnull final Locale aContentLocale)
  {
    return m_eDisplayText.getDisplayText (aContentLocale);
  }

  @Nullable
  public static ESearchOperator getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (ESearchOperator.class, sID);
  }
}
