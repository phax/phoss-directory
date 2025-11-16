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
package com.helger.pd.publisher.search;

import java.util.EnumSet;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.CodingStyleguideUnaware;
import com.helger.annotation.style.ReturnsMutableCopy;

import jakarta.annotation.Nullable;

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

  private ESearchDataType (@NonNull @Nonempty final EnumSet <ESearchOperator> aAllowedOperators)
  {
    m_aAllowedOperators = aAllowedOperators;
  }

  @NonNull
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
