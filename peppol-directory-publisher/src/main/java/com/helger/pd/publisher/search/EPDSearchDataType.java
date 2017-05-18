package com.helger.pd.publisher.search;

import java.util.EnumSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.CodingStyleguideUnaware;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;

/**
 * The search data types available.
 *
 * @author Philip Helger
 */
public enum EPDSearchDataType
{
  /** Case sensitive string */
  STRING_CS (EnumSet.of (EPDSearchOperator.EQ, EPDSearchOperator.NE, EPDSearchOperator.LT, EPDSearchOperator.LE, EPDSearchOperator.GT, EPDSearchOperator.GE, EPDSearchOperator.EMPTY, EPDSearchOperator.NOT_EMPTY, EPDSearchOperator.STRING_CONTAINS, EPDSearchOperator.STRING_STARTS_WITH, EPDSearchOperator.STRING_ENDS_WITH, EPDSearchOperator.STRING_REGEX)),
  /** Case insensitive string */
  STRING_CI (EnumSet.of (EPDSearchOperator.EQ, EPDSearchOperator.NE, EPDSearchOperator.LT, EPDSearchOperator.LE, EPDSearchOperator.GT, EPDSearchOperator.GE, EPDSearchOperator.EMPTY, EPDSearchOperator.NOT_EMPTY, EPDSearchOperator.STRING_CONTAINS, EPDSearchOperator.STRING_STARTS_WITH, EPDSearchOperator.STRING_ENDS_WITH, EPDSearchOperator.STRING_REGEX)),
  /** Integer/Long/BigInteger */
  INT (EnumSet.of (EPDSearchOperator.EQ, EPDSearchOperator.NE, EPDSearchOperator.LT, EPDSearchOperator.LE, EPDSearchOperator.GT, EPDSearchOperator.GE, EPDSearchOperator.EMPTY, EPDSearchOperator.NOT_EMPTY, EPDSearchOperator.STRING_CONTAINS, EPDSearchOperator.STRING_STARTS_WITH, EPDSearchOperator.STRING_ENDS_WITH, EPDSearchOperator.STRING_REGEX, EPDSearchOperator.INT_EVEN, EPDSearchOperator.INT_ODD)),
  /** Float/Double/BigDecimal */
  DOUBLE (EnumSet.of (EPDSearchOperator.EQ, EPDSearchOperator.NE, EPDSearchOperator.LT, EPDSearchOperator.LE, EPDSearchOperator.GT, EPDSearchOperator.GE, EPDSearchOperator.EMPTY, EPDSearchOperator.NOT_EMPTY, EPDSearchOperator.STRING_CONTAINS, EPDSearchOperator.STRING_STARTS_WITH, EPDSearchOperator.STRING_ENDS_WITH, EPDSearchOperator.STRING_REGEX)),
  /** LocalDate */
  DATE (EnumSet.of (EPDSearchOperator.EQ, EPDSearchOperator.NE, EPDSearchOperator.LT, EPDSearchOperator.LE, EPDSearchOperator.GT, EPDSearchOperator.GE, EPDSearchOperator.EMPTY, EPDSearchOperator.NOT_EMPTY, EPDSearchOperator.DATE_YEAR, EPDSearchOperator.DATE_MONTH, EPDSearchOperator.DATE_DAY, EPDSearchOperator.DATE_YEAR_MONTH, EPDSearchOperator.DATE_MONTH_DAY)),
  /** LocalTime */
  TIME (EnumSet.of (EPDSearchOperator.EQ, EPDSearchOperator.NE, EPDSearchOperator.LT, EPDSearchOperator.LE, EPDSearchOperator.GT, EPDSearchOperator.GE, EPDSearchOperator.EMPTY, EPDSearchOperator.NOT_EMPTY, EPDSearchOperator.TIME_HOUR, EPDSearchOperator.TIME_MINUTE, EPDSearchOperator.TIME_SECOND)),
  /** Boolean */
  BOOLEAN (EnumSet.of (EPDSearchOperator.EQ, EPDSearchOperator.NE, EPDSearchOperator.EMPTY, EPDSearchOperator.NOT_EMPTY));

  @CodingStyleguideUnaware
  private final EnumSet <EPDSearchOperator> m_aAllowedOperators;

  private EPDSearchDataType (@Nonnull @Nonempty final EnumSet <EPDSearchOperator> aAllowedOperators)
  {
    m_aAllowedOperators = aAllowedOperators;
  }

  @Nonnull
  @ReturnsMutableCopy
  public EnumSet <EPDSearchOperator> getAllAllowedOperators ()
  {
    return EnumSet.copyOf (m_aAllowedOperators);
  }

  public boolean isAllowedOperator (@Nullable final EPDSearchOperator eOperator)
  {
    return eOperator != null && m_aAllowedOperators.contains (eOperator);
  }
}
