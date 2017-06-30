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
public enum ESearchDataType
{
  /** Case sensitive string */
  STRING_CS (EnumSet.of (ESearchOperator.EQ,
                         ESearchOperator.NE,
                         ESearchOperator.LT,
                         ESearchOperator.LE,
                         ESearchOperator.GT,
                         ESearchOperator.GE,
                         ESearchOperator.EMPTY,
                         ESearchOperator.NOT_EMPTY,
                         ESearchOperator.STRING_CONTAINS,
                         ESearchOperator.STRING_STARTS_WITH,
                         ESearchOperator.STRING_ENDS_WITH,
                         ESearchOperator.STRING_REGEX)),
  /** Case insensitive string */
  STRING_CI (EnumSet.of (ESearchOperator.EQ,
                         ESearchOperator.NE,
                         ESearchOperator.LT,
                         ESearchOperator.LE,
                         ESearchOperator.GT,
                         ESearchOperator.GE,
                         ESearchOperator.EMPTY,
                         ESearchOperator.NOT_EMPTY,
                         ESearchOperator.STRING_CONTAINS,
                         ESearchOperator.STRING_STARTS_WITH,
                         ESearchOperator.STRING_ENDS_WITH,
                         ESearchOperator.STRING_REGEX)),
  /** Integer/Long/BigInteger */
  INT (EnumSet.of (ESearchOperator.EQ,
                   ESearchOperator.NE,
                   ESearchOperator.LT,
                   ESearchOperator.LE,
                   ESearchOperator.GT,
                   ESearchOperator.GE,
                   ESearchOperator.EMPTY,
                   ESearchOperator.NOT_EMPTY,
                   ESearchOperator.STRING_CONTAINS,
                   ESearchOperator.STRING_STARTS_WITH,
                   ESearchOperator.STRING_ENDS_WITH,
                   ESearchOperator.STRING_REGEX,
                   ESearchOperator.INT_EVEN,
                   ESearchOperator.INT_ODD)),
  /** Float/Double/BigDecimal */
  DOUBLE (EnumSet.of (ESearchOperator.EQ,
                      ESearchOperator.NE,
                      ESearchOperator.LT,
                      ESearchOperator.LE,
                      ESearchOperator.GT,
                      ESearchOperator.GE,
                      ESearchOperator.EMPTY,
                      ESearchOperator.NOT_EMPTY,
                      ESearchOperator.STRING_CONTAINS,
                      ESearchOperator.STRING_STARTS_WITH,
                      ESearchOperator.STRING_ENDS_WITH,
                      ESearchOperator.STRING_REGEX)),
  /** LocalDate */
  DATE (EnumSet.of (ESearchOperator.EQ,
                    ESearchOperator.NE,
                    ESearchOperator.LT,
                    ESearchOperator.LE,
                    ESearchOperator.GT,
                    ESearchOperator.GE,
                    ESearchOperator.EMPTY,
                    ESearchOperator.NOT_EMPTY,
                    ESearchOperator.DATE_YEAR,
                    ESearchOperator.DATE_MONTH,
                    ESearchOperator.DATE_DAY,
                    ESearchOperator.DATE_YEAR_MONTH,
                    ESearchOperator.DATE_MONTH_DAY)),
  /** LocalTime */
  TIME (EnumSet.of (ESearchOperator.EQ,
                    ESearchOperator.NE,
                    ESearchOperator.LT,
                    ESearchOperator.LE,
                    ESearchOperator.GT,
                    ESearchOperator.GE,
                    ESearchOperator.EMPTY,
                    ESearchOperator.NOT_EMPTY,
                    ESearchOperator.TIME_HOUR,
                    ESearchOperator.TIME_MINUTE,
                    ESearchOperator.TIME_SECOND)),
  /** Boolean */
  BOOLEAN (EnumSet.of (ESearchOperator.EQ, ESearchOperator.NE, ESearchOperator.EMPTY, ESearchOperator.NOT_EMPTY));

  @CodingStyleguideUnaware
  private final EnumSet <ESearchOperator> m_aAllowedOperators;

  private ESearchDataType (@Nonnull @Nonempty final EnumSet <ESearchOperator> aAllowedOperators)
  {
    m_aAllowedOperators = aAllowedOperators;
  }

  @Nonnull
  @ReturnsMutableCopy
  public EnumSet <ESearchOperator> getAllAllowedOperators ()
  {
    return EnumSet.copyOf (m_aAllowedOperators);
  }

  public boolean isAllowedOperator (@Nullable final ESearchOperator eOperator)
  {
    return eOperator != null && m_aAllowedOperators.contains (eOperator);
  }
}
