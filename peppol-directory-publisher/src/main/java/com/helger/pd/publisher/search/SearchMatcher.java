/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.YearMonth;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.CGlobal;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.compare.CompareHelper;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.math.MathHelper;
import com.helger.commons.regex.RegExHelper;
import com.helger.commons.string.StringHelper;
import com.helger.commons.typeconvert.TypeConverter;
import com.helger.commons.typeconvert.TypeConverterException;

/**
 * Generic matcher. Compares a search value using a specified operator on a
 * reference values that has a certain data type.
 *
 * @author Philip Helger
 */
public final class SearchMatcher
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (SearchMatcher.class);

  private SearchMatcher ()
  {}

  @Nullable
  public static String unify (@Nullable final String s)
  {
    return s == null ? null : s.toUpperCase (Locale.US);
  }

  public static boolean matchesString (@Nullable final String sReferenceValue,
                                       @Nonnull final ESearchOperator eOperator,
                                       @Nullable final String sSearchValue)
  {
    switch (eOperator)
    {
      case EQ:
        return EqualsHelper.equals (sReferenceValue, sSearchValue);
      case NE:
        return !EqualsHelper.equals (sReferenceValue, sSearchValue);
      case LT:
        return CompareHelper.compare (sSearchValue, sReferenceValue) < 0;
      case LE:
        return CompareHelper.compare (sSearchValue, sReferenceValue) <= 0;
      case GT:
        return CompareHelper.compare (sSearchValue, sReferenceValue) > 0;
      case GE:
        return CompareHelper.compare (sSearchValue, sReferenceValue) >= 0;
      case EMPTY:
        return StringHelper.hasNoText (sReferenceValue);
      case NOT_EMPTY:
        return StringHelper.hasText (sReferenceValue);
      case STRING_CONTAINS:
        // Different semantics than StringHelper.contains for empty values!
        return StringHelper.hasText (sReferenceValue) &&
               StringHelper.hasText (sSearchValue) &&
               sReferenceValue.contains (sSearchValue);
      case STRING_STARTS_WITH:
        // Different semantics than StringHelper.startsWith for empty values!
        return StringHelper.hasText (sReferenceValue) &&
               StringHelper.hasText (sSearchValue) &&
               sReferenceValue.startsWith (sSearchValue);
      case STRING_ENDS_WITH:
        // Different semantics than StringHelper.endsWith for empty values!
        return StringHelper.hasText (sReferenceValue) &&
               StringHelper.hasText (sSearchValue) &&
               sReferenceValue.endsWith (sSearchValue);
      case STRING_REGEX:
        return StringHelper.hasText (sReferenceValue) &&
               StringHelper.hasText (sSearchValue) &&
               RegExHelper.stringMatchesPattern (sSearchValue, sReferenceValue);
      default:
        throw new IllegalArgumentException ("Unsupported String search operator " + eOperator);
    }
  }

  public static boolean matchesInt (@Nullable final BigInteger aReferenceValue,
                                    @Nonnull final ESearchOperator eOperator,
                                    @Nullable final Object aSearchValue)
  {
    switch (eOperator)
    {
      case EQ:
        return EqualsHelper.equals (aReferenceValue, TypeConverter.convert (aSearchValue, BigInteger.class));
      case NE:
        return !EqualsHelper.equals (aReferenceValue,
                                     TypeConverter.convert (aSearchValue, BigInteger.class));
      case LT:
        return CompareHelper.compare (TypeConverter.convert (aSearchValue, BigInteger.class),
                                      aReferenceValue) < 0;
      case LE:
        return CompareHelper.compare (TypeConverter.convert (aSearchValue, BigInteger.class),
                                      aReferenceValue) <= 0;
      case GT:
        return CompareHelper.compare (TypeConverter.convert (aSearchValue, BigInteger.class),
                                      aReferenceValue) > 0;
      case GE:
        return CompareHelper.compare (TypeConverter.convert (aSearchValue, BigInteger.class),
                                      aReferenceValue) >= 0;
      case EMPTY:
        return aReferenceValue == null;
      case NOT_EMPTY:
        return aReferenceValue != null;
      case STRING_CONTAINS:
      case STRING_STARTS_WITH:
      case STRING_ENDS_WITH:
      case STRING_REGEX:
        // Use string version :)
        return matchesString (aReferenceValue == null ? null : aReferenceValue.toString (),
                              eOperator,
                              TypeConverter.convert (aSearchValue, String.class));
      case INT_EVEN:
        return aReferenceValue != null && MathHelper.isEQ0 (aReferenceValue.mod (CGlobal.BIGINT_2));
      case INT_ODD:
        return aReferenceValue != null && MathHelper.isNE0 (aReferenceValue.mod (CGlobal.BIGINT_2));
      default:
        throw new IllegalArgumentException ("Unsupported Int search operator " + eOperator);
    }
  }

  public static boolean matchesDouble (@Nullable final BigDecimal aReferenceValue,
                                       @Nonnull final ESearchOperator eOperator,
                                       @Nullable final Object aSearchValue)
  {
    switch (eOperator)
    {
      case EQ:
        return EqualsHelper.equals (aReferenceValue, TypeConverter.convert (aSearchValue, BigDecimal.class));
      case NE:
        return !EqualsHelper.equals (aReferenceValue,
                                     TypeConverter.convert (aSearchValue, BigDecimal.class));
      case LT:
        return CompareHelper.compare (TypeConverter.convert (aSearchValue, BigDecimal.class),
                                      aReferenceValue) < 0;
      case LE:
        return CompareHelper.compare (TypeConverter.convert (aSearchValue, BigDecimal.class),
                                      aReferenceValue) <= 0;
      case GT:
        return CompareHelper.compare (TypeConverter.convert (aSearchValue, BigDecimal.class),
                                      aReferenceValue) > 0;
      case GE:
        return CompareHelper.compare (TypeConverter.convert (aSearchValue, BigDecimal.class),
                                      aReferenceValue) >= 0;
      case EMPTY:
        return aReferenceValue == null;
      case NOT_EMPTY:
        return aReferenceValue != null;
      case STRING_CONTAINS:
      case STRING_STARTS_WITH:
      case STRING_ENDS_WITH:
      case STRING_REGEX:
        // Use string version :)
        return matchesString (aReferenceValue == null ? null : aReferenceValue.toString (),
                              eOperator,
                              TypeConverter.convert (aSearchValue, String.class));
      default:
        throw new IllegalArgumentException ("Unsupported double search operator " + eOperator);
    }
  }

  public static boolean matchesDate (@Nullable final LocalDate aReferenceValue,
                                     @Nonnull final ESearchOperator eOperator,
                                     @Nullable final Object aSearchValue)
  {
    switch (eOperator)
    {
      case EQ:
        return EqualsHelper.equals (aReferenceValue, aSearchValue);
      case NE:
        return !EqualsHelper.equals (aReferenceValue, aSearchValue);
      case LT:
        return CompareHelper.compare (TypeConverter.convert (aSearchValue, LocalDate.class),
                                      aReferenceValue) < 0;
      case LE:
        return CompareHelper.compare (TypeConverter.convert (aSearchValue, LocalDate.class),
                                      aReferenceValue) <= 0;
      case GT:
        return CompareHelper.compare (TypeConverter.convert (aSearchValue, LocalDate.class),
                                      aReferenceValue) > 0;
      case GE:
        return CompareHelper.compare (TypeConverter.convert (aSearchValue, LocalDate.class),
                                      aReferenceValue) >= 0;
      case EMPTY:
        return aReferenceValue == null;
      case NOT_EMPTY:
        return aReferenceValue != null;
      case DATE_YEAR:
        return aReferenceValue != null && aReferenceValue.getYear () == TypeConverter.convertToInt (aSearchValue);
      case DATE_MONTH:
        return aReferenceValue != null && aReferenceValue.getMonthValue () == TypeConverter.convertToInt (aSearchValue);
      case DATE_DAY:
        return aReferenceValue != null && aReferenceValue.getDayOfMonth () == TypeConverter.convertToInt (aSearchValue);
      case DATE_YEAR_MONTH:
        return aReferenceValue != null &&
               YearMonth.from (aReferenceValue)
                        .equals (TypeConverter.convert (aSearchValue, YearMonth.class));
      case DATE_MONTH_DAY:
        return aReferenceValue != null &&
               MonthDay.from (aReferenceValue).equals (TypeConverter.convert (aSearchValue, MonthDay.class));
      default:
        throw new IllegalArgumentException ("Unsupported LocalDate search operator " + eOperator);
    }
  }

  public static boolean matchesTime (@Nullable final LocalTime aReferenceValue,
                                     @Nonnull final ESearchOperator eOperator,
                                     @Nullable final Object aSearchValue)
  {
    switch (eOperator)
    {
      case EQ:
        return EqualsHelper.equals (aReferenceValue, aSearchValue);
      case NE:
        return !EqualsHelper.equals (aReferenceValue, aSearchValue);
      case LT:
        return CompareHelper.compare (TypeConverter.convert (aSearchValue, LocalTime.class),
                                      aReferenceValue) < 0;
      case LE:
        return CompareHelper.compare (TypeConverter.convert (aSearchValue, LocalTime.class),
                                      aReferenceValue) <= 0;
      case GT:
        return CompareHelper.compare (TypeConverter.convert (aSearchValue, LocalTime.class),
                                      aReferenceValue) > 0;
      case GE:
        return CompareHelper.compare (TypeConverter.convert (aSearchValue, LocalTime.class),
                                      aReferenceValue) >= 0;
      case EMPTY:
        return aReferenceValue == null;
      case NOT_EMPTY:
        return aReferenceValue != null;
      case TIME_HOUR:
        return aReferenceValue != null && aReferenceValue.getHour () == TypeConverter.convertToInt (aSearchValue);
      case TIME_MINUTE:
        return aReferenceValue != null && aReferenceValue.getMinute () == TypeConverter.convertToInt (aSearchValue);
      case TIME_SECOND:
        return aReferenceValue != null && aReferenceValue.getSecond () == TypeConverter.convertToInt (aSearchValue);
      default:
        throw new IllegalArgumentException ("Unsupported LocalTime search operator " + eOperator);
    }
  }

  public static boolean matchesBoolean (@Nullable final Boolean aReferenceValue,
                                        @Nonnull final ESearchOperator eOperator,
                                        @Nullable final Boolean aSearchValue)
  {
    switch (eOperator)
    {
      case EQ:
        return EqualsHelper.equals (aReferenceValue, aSearchValue);
      case NE:
        return !EqualsHelper.equals (aReferenceValue, aSearchValue);
      case EMPTY:
        return aReferenceValue == null;
      case NOT_EMPTY:
        return aReferenceValue != null;
      default:
        throw new IllegalArgumentException ("Unsupported Boolean search operator " + eOperator);
    }
  }

  public static boolean matches (@Nullable final Object aReferenceValue,
                                 @Nonnull final ESearchDataType eDataType,
                                 @Nonnull final ESearchOperator eOperator,
                                 @Nullable final Object aSearchValue)
  {
    ValueEnforcer.notNull (eDataType, "DataType");
    ValueEnforcer.notNull (eOperator, "Operator");
    ValueEnforcer.isTrue (eDataType.isAllowedOperator (eOperator),
                          () -> "The operator " + eOperator + " is not valid for data type " + eDataType);

    try
    {
      switch (eDataType)
      {
        case STRING_CS:
          return matchesString (TypeConverter.convert (aReferenceValue, String.class),
                                eOperator,
                                TypeConverter.convert (aSearchValue, String.class));
        case STRING_CI:
          return matchesString (unify (TypeConverter.convert (aReferenceValue, String.class)),
                                eOperator,
                                unify (TypeConverter.convert (aSearchValue, String.class)));
        case INT:
          return matchesInt (TypeConverter.convert (aReferenceValue, BigInteger.class),
                             eOperator,
                             aSearchValue);
        case DOUBLE:
          return matchesDouble (TypeConverter.convert (aReferenceValue, BigDecimal.class),
                                eOperator,
                                aSearchValue);
        case DATE:
          return matchesDate (TypeConverter.convert (aReferenceValue, LocalDate.class),
                              eOperator,
                              aSearchValue);
        case TIME:
          return matchesTime (TypeConverter.convert (aReferenceValue, LocalTime.class),
                              eOperator,
                              aSearchValue);
        case BOOLEAN:
          return matchesBoolean (TypeConverter.convert (aReferenceValue, Boolean.class),
                                 eOperator,
                                 TypeConverter.convert (aSearchValue, Boolean.class));
        default:
          throw new IllegalStateException ("Unsupported data type: " + eDataType);
      }
    }
    catch (final TypeConverterException ex)
    {
      // Doesn't matter - doesn't match
      if (s_aLogger.isDebugEnabled ())
        s_aLogger.debug ("Type conversion failed", ex);
      return false;
    }
  }
}
