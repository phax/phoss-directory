package com.helger.pd.publisher.search;

/**
 * The search data types available.
 *
 * @author Philip Helger
 */
public enum EPDSearchDataType
{
  /** Case sensitive string */
  STRING_CS,
  /** Case insensitive string */
  STRING_CI,
  /** Integer/Long/BigInteger */
  INT,
  /** Float/Double/BigDecimal */
  DOUBLE,
  /** LocalDate */
  DATE,
  /** LocalTime */
  TIME,
  /** Boolean */
  BOOLEAN;
}
