package com.helger.pd.publisher.search;

import java.time.MonthDay;
import java.time.YearMonth;

import javax.annotation.Nullable;

public enum EPDSearchOperator
{
  /** Equals */
  EQ (true, null),
  /** Not equals */
  NE (true, null),
  /** Lower than */
  LT (true, null),
  /** Lower or equal */
  LE (true, null),
  /** Greater than */
  GT (true, null),
  /** Greater or equal */
  GE (true, null),
  /** Is empty */
  EMPTY (false, null),
  /** Is not empty */
  NOT_EMPTY (false, null),
  /** String contains */
  STRING_CONTAINS (true, String.class),
  /** String starts with */
  STRING_STARTS_WITH (true, String.class),
  /** String ends with */
  STRING_ENDS_WITH (true, String.class),
  /** String matches regular expression */
  STRING_REGEX (true, String.class),
  /** Int even */
  INT_EVEN (false, null),
  /** Int odd */
  INT_ODD (false, null),
  /** Year of date */
  DATE_YEAR (true, int.class),
  /** Month of date */
  DATE_MONTH (true, int.class),
  /** Day of date */
  DATE_DAY (true, int.class),
  /** Year and month of date */
  DATE_YEAR_MONTH (true, YearMonth.class),
  /** Month and day of date */
  DATE_MONTH_DAY (true, MonthDay.class),
  /** Hour of time */
  TIME_HOUR (true, int.class),
  /** Minute of time */
  TIME_MINUTE (true, int.class),
  /** Second of time */
  TIME_SECOND (true, int.class);

  private final boolean m_bNeedsValue;
  private final Class <?> m_aSpecialValueClass;

  private EPDSearchOperator (final boolean bNeedsValue, @Nullable final Class <?> aSpecialValueClass)
  {
    m_bNeedsValue = bNeedsValue;
    m_aSpecialValueClass = aSpecialValueClass;
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
}
