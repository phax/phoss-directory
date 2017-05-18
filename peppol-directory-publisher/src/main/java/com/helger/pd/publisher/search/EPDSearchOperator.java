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

public enum EPDSearchOperator implements IHasDisplayText, IHasID <String>
{
  /** Equals */
  EQ ("eq", true, null, EPDSearchOperatorText.EQ),
  /** Not equals */
  NE ("ne", true, null, EPDSearchOperatorText.NE),
  /** Lower than */
  LT ("lt", true, null, EPDSearchOperatorText.LT),
  /** Lower or equal */
  LE ("le", true, null, EPDSearchOperatorText.LE),
  /** Greater than */
  GT ("gt", true, null, EPDSearchOperatorText.GT),
  /** Greater or equal */
  GE ("ge", true, null, EPDSearchOperatorText.GE),
  /** Is empty */
  EMPTY ("empty", false, null, EPDSearchOperatorText.EMPTY),
  /** Is not empty */
  NOT_EMPTY ("notempty", false, null, EPDSearchOperatorText.NOT_EMPTY),
  /** String contains */
  STRING_CONTAINS ("contains", true, String.class, EPDSearchOperatorText.STRING_CONTAINS),
  /** String starts with */
  STRING_STARTS_WITH ("startswith", true, String.class, EPDSearchOperatorText.STRING_STARTS_WITH),
  /** String ends with */
  STRING_ENDS_WITH ("endswith", true, String.class, EPDSearchOperatorText.STRING_ENDS_WITH),
  /** String matches regular expression */
  STRING_REGEX ("regex", true, String.class, EPDSearchOperatorText.STRING_REGEX),
  /** Int even */
  INT_EVEN ("even", false, null, EPDSearchOperatorText.INT_EVEN),
  /** Int odd */
  INT_ODD ("odd", false, null, EPDSearchOperatorText.INT_ODD),
  /** Year of date */
  DATE_YEAR ("year", true, int.class, EPDSearchOperatorText.DATE_YEAR),
  /** Month of date */
  DATE_MONTH ("month", true, int.class, EPDSearchOperatorText.DATE_MONTH),
  /** Day of date */
  DATE_DAY ("day", true, int.class, EPDSearchOperatorText.DATE_DAY),
  /** Year and month of date */
  DATE_YEAR_MONTH ("yearmonth", true, YearMonth.class, EPDSearchOperatorText.DATE_YEAR_MONTH),
  /** Month and day of date */
  DATE_MONTH_DAY ("monthday", true, MonthDay.class, EPDSearchOperatorText.DATE_MONTH_DAY),
  /** Hour of time */
  TIME_HOUR ("hour", true, int.class, EPDSearchOperatorText.TIME_HOUR),
  /** Minute of time */
  TIME_MINUTE ("minute", true, int.class, EPDSearchOperatorText.TIME_MINUTE),
  /** Second of time */
  TIME_SECOND ("second", true, int.class, EPDSearchOperatorText.TIME_SECOND);

  private final String m_sID;
  private final boolean m_bNeedsValue;
  private final Class <?> m_aSpecialValueClass;
  private final EPDSearchOperatorText m_eDisplayText;

  private EPDSearchOperator (@Nonnull @Nonempty final String sID,
                             final boolean bNeedsValue,
                             @Nullable final Class <?> aSpecialValueClass,
                             @Nonnull final EPDSearchOperatorText eDisplayText)
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
  public static EPDSearchOperator getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (EPDSearchOperator.class, sID);
  }
}
