package com.helger.pd.publisher.search;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.MonthDay;
import java.time.YearMonth;

import org.junit.Test;

import com.helger.commons.datetime.PDTFactory;
import com.helger.datetime.CPDT;

/**
 * Test class for class {@link SearchMatcher}.
 *
 * @author Philip Helger
 */
public final class PDMatcherTest
{
  @Test
  public void testStringCS ()
  {
    final ESearchDataType eDataType = ESearchDataType.STRING_CS;

    ESearchOperator eOperator = ESearchOperator.EQ;
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, "bl"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "bla"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, "la"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, ""));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, "bla"));
    assertFalse (SearchMatcher.matches ("", eDataType, eOperator, "bla"));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.NE;
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "bl"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, "bla"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "la"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, ""));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, "bla"));
    assertTrue (SearchMatcher.matches ("", eDataType, eOperator, "bla"));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.LT;
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "bl"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, "bla"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, "la"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, ""));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, "bla"));
    assertFalse (SearchMatcher.matches ("", eDataType, eOperator, "bla"));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.LE;
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "bl"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "bla"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, "la"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, ""));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, "bla"));
    assertFalse (SearchMatcher.matches ("", eDataType, eOperator, "bla"));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.GT;
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, "bl"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, "bla"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "la"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, ""));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, "bla"));
    assertTrue (SearchMatcher.matches ("", eDataType, eOperator, "bla"));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.GE;
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, "bl"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "bla"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "la"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, ""));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, "bla"));
    assertTrue (SearchMatcher.matches ("", eDataType, eOperator, "bla"));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.EMPTY;
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches ("", eDataType, eOperator, null));

    eOperator = ESearchOperator.NOT_EMPTY;
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches ("", eDataType, eOperator, null));

    eOperator = ESearchOperator.STRING_CONTAINS;
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "bl"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "bla"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "la"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, ""));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, "bla"));
    assertFalse (SearchMatcher.matches ("", eDataType, eOperator, "bla"));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.STRING_STARTS_WITH;
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "bl"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "bla"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, "la"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, ""));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, "bla"));
    assertFalse (SearchMatcher.matches ("", eDataType, eOperator, "bla"));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.STRING_ENDS_WITH;
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, "bl"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "bla"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "la"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, ""));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, "bla"));
    assertFalse (SearchMatcher.matches ("", eDataType, eOperator, "bla"));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.STRING_REGEX;
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, "bl"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "bla"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, "la"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, ""));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, "bla"));
    assertFalse (SearchMatcher.matches ("", eDataType, eOperator, "bla"));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, ".*la?"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, "[0-9]*"));
  }

  @Test
  public void testStringCI_CS ()
  {
    final ESearchDataType eDataType = ESearchDataType.STRING_CI;

    ESearchOperator eOperator = ESearchOperator.EQ;
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, "bl"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "bla"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, "la"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, ""));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, "bla"));
    assertFalse (SearchMatcher.matches ("", eDataType, eOperator, "bla"));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.NE;
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "bl"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, "bla"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "la"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, ""));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, "bla"));
    assertTrue (SearchMatcher.matches ("", eDataType, eOperator, "bla"));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.LT;
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "bl"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, "bla"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, "la"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, ""));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, "bla"));
    assertFalse (SearchMatcher.matches ("", eDataType, eOperator, "bla"));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.LE;
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "bl"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "bla"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, "la"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, ""));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, "bla"));
    assertFalse (SearchMatcher.matches ("", eDataType, eOperator, "bla"));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.GT;
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, "bl"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, "bla"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "la"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, ""));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, "bla"));
    assertTrue (SearchMatcher.matches ("", eDataType, eOperator, "bla"));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.GE;
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, "bl"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "bla"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "la"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, ""));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, "bla"));
    assertTrue (SearchMatcher.matches ("", eDataType, eOperator, "bla"));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.EMPTY;
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches ("", eDataType, eOperator, null));

    eOperator = ESearchOperator.NOT_EMPTY;
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches ("", eDataType, eOperator, null));

    eOperator = ESearchOperator.STRING_CONTAINS;
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "bl"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "bla"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "la"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, ""));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, "bla"));
    assertFalse (SearchMatcher.matches ("", eDataType, eOperator, "bla"));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.STRING_STARTS_WITH;
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "bl"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "bla"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, "la"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, ""));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, "bla"));
    assertFalse (SearchMatcher.matches ("", eDataType, eOperator, "bla"));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.STRING_ENDS_WITH;
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, "bl"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "bla"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "la"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, ""));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, "bla"));
    assertFalse (SearchMatcher.matches ("", eDataType, eOperator, "bla"));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.STRING_REGEX;
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, "bl"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "bla"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, "la"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, ""));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, "bla"));
    assertFalse (SearchMatcher.matches ("", eDataType, eOperator, "bla"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, ".*la?"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, "[0-9]*"));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));
  }

  @Test
  public void testStringCI_CI ()
  {
    final ESearchDataType eDataType = ESearchDataType.STRING_CI;

    ESearchOperator eOperator = ESearchOperator.EQ;
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, "BL"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "BLA"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, "LA"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, ""));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, "BLA"));
    assertFalse (SearchMatcher.matches ("", eDataType, eOperator, "BLA"));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.NE;
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "BL"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, "BLA"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "LA"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, ""));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, "BLA"));
    assertTrue (SearchMatcher.matches ("", eDataType, eOperator, "BLA"));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.LT;
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "BL"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, "BLA"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, "LA"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, ""));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, "BLA"));
    assertFalse (SearchMatcher.matches ("", eDataType, eOperator, "BLA"));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.LE;
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "BL"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "BLA"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, "LA"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, ""));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, "BLA"));
    assertFalse (SearchMatcher.matches ("", eDataType, eOperator, "BLA"));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.GT;
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, "BL"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, "BLA"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "LA"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, ""));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, "BLA"));
    assertTrue (SearchMatcher.matches ("", eDataType, eOperator, "BLA"));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.GE;
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, "BL"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "BLA"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "LA"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, ""));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, "BLA"));
    assertTrue (SearchMatcher.matches ("", eDataType, eOperator, "BLA"));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.EMPTY;
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches ("", eDataType, eOperator, null));

    eOperator = ESearchOperator.NOT_EMPTY;
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches ("", eDataType, eOperator, null));

    eOperator = ESearchOperator.STRING_CONTAINS;
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "BL"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "BLA"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "LA"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, ""));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, "BLA"));
    assertFalse (SearchMatcher.matches ("", eDataType, eOperator, "BLA"));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.STRING_STARTS_WITH;
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "BL"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "BLA"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, "LA"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, ""));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, "BLA"));
    assertFalse (SearchMatcher.matches ("", eDataType, eOperator, "BLA"));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.STRING_ENDS_WITH;
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, "BL"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "BLA"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "LA"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, ""));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, "BLA"));
    assertFalse (SearchMatcher.matches ("", eDataType, eOperator, "BLA"));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.STRING_REGEX;
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, "BL"));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, "BLA"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, "LA"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, ""));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, "BLA"));
    assertFalse (SearchMatcher.matches ("", eDataType, eOperator, "BLA"));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches ("bla", eDataType, eOperator, ".*LA?"));
    assertFalse (SearchMatcher.matches ("bla", eDataType, eOperator, "[0-9]*"));
  }

  @SuppressWarnings ("boxing")
  @Test
  public void testLong ()
  {
    final ESearchDataType eDataType = ESearchDataType.INT;

    ESearchOperator eOperator = ESearchOperator.EQ;
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, 1));
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, 123));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, 23));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, 0));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, 123));
    assertFalse (SearchMatcher.matches (0, eDataType, eOperator, 123));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.NE;
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, 1));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, 123));
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, 23));
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, 0));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, 123));
    assertTrue (SearchMatcher.matches (0, eDataType, eOperator, 123));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.LT;
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, 1));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, 123));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, 234));
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, 0));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, 123));
    assertFalse (SearchMatcher.matches (0, eDataType, eOperator, 123));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.LE;
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, 1));
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, 123));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, 234));
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, 0));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, 123));
    assertFalse (SearchMatcher.matches (0, eDataType, eOperator, 123));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.GT;
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, 1));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, 123));
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, 234));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, 0));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, 123));
    assertTrue (SearchMatcher.matches (0, eDataType, eOperator, 123));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.GE;
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, 1));
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, 123));
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, 234));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, 0));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, 123));
    assertTrue (SearchMatcher.matches (0, eDataType, eOperator, 123));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.EMPTY;
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (0, eDataType, eOperator, null));

    eOperator = ESearchOperator.NOT_EMPTY;
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches (0, eDataType, eOperator, null));

    eOperator = ESearchOperator.INT_EVEN;
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches (0, eDataType, eOperator, null));

    eOperator = ESearchOperator.INT_ODD;
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (0, eDataType, eOperator, null));

    eOperator = ESearchOperator.STRING_CONTAINS;
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, 1));
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, 123));
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, 23));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, 0));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, 123));
    assertFalse (SearchMatcher.matches (0, eDataType, eOperator, 123));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.STRING_STARTS_WITH;
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, 1));
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, 123));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, 23));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, 0));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, 123));
    assertFalse (SearchMatcher.matches (0, eDataType, eOperator, 123));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.STRING_ENDS_WITH;
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, 1));
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, 123));
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, 23));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, 0));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, 123));
    assertFalse (SearchMatcher.matches (0, eDataType, eOperator, 123));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.STRING_REGEX;
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, 1));
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, 123));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, 23));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, 0));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, 123));
    assertFalse (SearchMatcher.matches (0, eDataType, eOperator, 123));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, "[1-3]+"));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, ".*4.*"));
  }

  @SuppressWarnings ("boxing")
  @Test
  public void testDouble ()
  {
    final ESearchDataType eDataType = ESearchDataType.DOUBLE;

    ESearchOperator eOperator = ESearchOperator.EQ;
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, 1));
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, 123));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, 23));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, 0));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, 123));
    assertFalse (SearchMatcher.matches (0, eDataType, eOperator, 123));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.NE;
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, 1));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, 123));
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, 23));
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, 0));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, 123));
    assertTrue (SearchMatcher.matches (0, eDataType, eOperator, 123));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.LT;
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, 1));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, 123));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, 234));
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, 0));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, 123));
    assertFalse (SearchMatcher.matches (0, eDataType, eOperator, 123));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.LE;
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, 1));
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, 123));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, 234));
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, 0));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, 123));
    assertFalse (SearchMatcher.matches (0, eDataType, eOperator, 123));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.GT;
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, 1));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, 123));
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, 234));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, 0));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, 123));
    assertTrue (SearchMatcher.matches (0, eDataType, eOperator, 123));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.GE;
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, 1));
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, 123));
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, 234));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, 0));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, 123));
    assertTrue (SearchMatcher.matches (0, eDataType, eOperator, 123));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.EMPTY;
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (0, eDataType, eOperator, null));

    eOperator = ESearchOperator.NOT_EMPTY;
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches (0, eDataType, eOperator, null));

    eOperator = ESearchOperator.STRING_CONTAINS;
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, 1));
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, 123));
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, 23));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, 0));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, 123));
    assertFalse (SearchMatcher.matches (0, eDataType, eOperator, 123));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.STRING_STARTS_WITH;
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, 1));
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, 123));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, 23));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, 0));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, 123));
    assertFalse (SearchMatcher.matches (0, eDataType, eOperator, 123));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.STRING_ENDS_WITH;
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, 1));
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, 123));
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, 23));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, 0));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, 123));
    assertFalse (SearchMatcher.matches (0, eDataType, eOperator, 123));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.STRING_REGEX;
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, 1));
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, 123));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, 23));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, 0));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, 123));
    assertFalse (SearchMatcher.matches (0, eDataType, eOperator, 123));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches (123, eDataType, eOperator, "[1-3]+"));
    assertFalse (SearchMatcher.matches (123, eDataType, eOperator, ".*4.*"));
  }

  @SuppressWarnings ("boxing")
  @Test
  public void testDate ()
  {
    final LocalDate aDate0 = PDTFactory.createLocalDate (0);
    final LocalDate aDate1 = PDTFactory.createLocalDate (2017, Month.FEBRUARY, 2);
    final LocalDate aDate2 = PDTFactory.createLocalDate (2017, Month.MARCH, 3);
    final LocalDate aDate3 = PDTFactory.createLocalDate (2017, Month.APRIL, 4);
    final ESearchDataType eDataType = ESearchDataType.DATE;

    ESearchOperator eOperator = ESearchOperator.EQ;
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, aDate1));
    assertTrue (SearchMatcher.matches (aDate2, eDataType, eOperator, aDate2));
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, aDate3));
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, aDate0));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, aDate2));
    assertFalse (SearchMatcher.matches (aDate0, eDataType, eOperator, aDate2));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, "crap"));

    eOperator = ESearchOperator.NE;
    assertTrue (SearchMatcher.matches (aDate2, eDataType, eOperator, aDate1));
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, aDate2));
    assertTrue (SearchMatcher.matches (aDate2, eDataType, eOperator, aDate3));
    assertTrue (SearchMatcher.matches (aDate2, eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches (aDate2, eDataType, eOperator, aDate0));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, aDate2));
    assertTrue (SearchMatcher.matches (aDate0, eDataType, eOperator, aDate2));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches (aDate2, eDataType, eOperator, "crap"));

    eOperator = ESearchOperator.LT;
    assertTrue (SearchMatcher.matches (aDate2, eDataType, eOperator, aDate1));
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, aDate2));
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, aDate3));
    assertTrue (SearchMatcher.matches (aDate2, eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches (aDate2, eDataType, eOperator, aDate0));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, aDate2));
    assertFalse (SearchMatcher.matches (aDate0, eDataType, eOperator, aDate2));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, "crap"));

    eOperator = ESearchOperator.LE;
    assertTrue (SearchMatcher.matches (aDate2, eDataType, eOperator, aDate1));
    assertTrue (SearchMatcher.matches (aDate2, eDataType, eOperator, aDate2));
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, aDate3));
    assertTrue (SearchMatcher.matches (aDate2, eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches (aDate2, eDataType, eOperator, aDate0));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, aDate2));
    assertFalse (SearchMatcher.matches (aDate0, eDataType, eOperator, aDate2));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, "crap"));

    eOperator = ESearchOperator.GT;
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, aDate1));
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, aDate2));
    assertTrue (SearchMatcher.matches (aDate2, eDataType, eOperator, aDate3));
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, aDate0));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, aDate2));
    assertTrue (SearchMatcher.matches (aDate0, eDataType, eOperator, aDate2));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, "crap"));

    eOperator = ESearchOperator.GE;
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, aDate1));
    assertTrue (SearchMatcher.matches (aDate2, eDataType, eOperator, aDate2));
    assertTrue (SearchMatcher.matches (aDate2, eDataType, eOperator, aDate3));
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, aDate0));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, aDate2));
    assertTrue (SearchMatcher.matches (aDate0, eDataType, eOperator, aDate2));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, "crap"));

    eOperator = ESearchOperator.EMPTY;
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (aDate0, eDataType, eOperator, null));

    eOperator = ESearchOperator.NOT_EMPTY;
    assertTrue (SearchMatcher.matches (aDate2, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches (aDate0, eDataType, eOperator, null));

    eOperator = ESearchOperator.DATE_YEAR;
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, 2016));
    assertTrue (SearchMatcher.matches (aDate2, eDataType, eOperator, 2017));
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, 2018));
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, aDate0));
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, "crap"));

    eOperator = ESearchOperator.DATE_MONTH;
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, Month.JANUARY));
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, 2));
    assertTrue (SearchMatcher.matches (aDate2, eDataType, eOperator, Month.MARCH));
    assertTrue (SearchMatcher.matches (aDate2, eDataType, eOperator, 3));
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, 4));
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, Month.DECEMBER));
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, aDate0));
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, "crap"));

    eOperator = ESearchOperator.DATE_DAY;
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, 2));
    assertTrue (SearchMatcher.matches (aDate2, eDataType, eOperator, 3));
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, 4));
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, aDate0));
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, "crap"));

    eOperator = ESearchOperator.DATE_YEAR_MONTH;
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, YearMonth.of (2017, Month.FEBRUARY)));
    assertTrue (SearchMatcher.matches (aDate2, eDataType, eOperator, YearMonth.of (2017, Month.MARCH)));
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, YearMonth.of (2017, Month.APRIL)));
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, 4));
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, aDate0));
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, "crap"));

    eOperator = ESearchOperator.DATE_MONTH_DAY;
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, MonthDay.of (Month.FEBRUARY, 2)));
    assertTrue (SearchMatcher.matches (aDate2, eDataType, eOperator, MonthDay.of (Month.MARCH, 3)));
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, MonthDay.of (Month.APRIL, 4)));
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, 4));
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, aDate0));
    assertFalse (SearchMatcher.matches (aDate2, eDataType, eOperator, "crap"));
  }

  @SuppressWarnings ("boxing")
  @Test
  public void testTime ()
  {
    final LocalTime aTime0 = CPDT.NULL_LOCAL_TIME;
    final LocalTime aTime1 = PDTFactory.createLocalTime (10, 11, 12);
    final LocalTime aTime2 = PDTFactory.createLocalTime (13, 14, 15);
    final LocalTime aTime3 = PDTFactory.createLocalTime (16, 17, 18);
    final ESearchDataType eDataType = ESearchDataType.TIME;

    ESearchOperator eOperator = ESearchOperator.EQ;
    assertFalse (SearchMatcher.matches (aTime2, eDataType, eOperator, aTime1));
    assertTrue (SearchMatcher.matches (aTime2, eDataType, eOperator, aTime2));
    assertFalse (SearchMatcher.matches (aTime2, eDataType, eOperator, aTime3));
    assertFalse (SearchMatcher.matches (aTime2, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (aTime2, eDataType, eOperator, aTime0));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, aTime2));
    assertFalse (SearchMatcher.matches (aTime0, eDataType, eOperator, aTime2));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (aTime2, eDataType, eOperator, "crap"));

    eOperator = ESearchOperator.NE;
    assertTrue (SearchMatcher.matches (aTime2, eDataType, eOperator, aTime1));
    assertFalse (SearchMatcher.matches (aTime2, eDataType, eOperator, aTime2));
    assertTrue (SearchMatcher.matches (aTime2, eDataType, eOperator, aTime3));
    assertTrue (SearchMatcher.matches (aTime2, eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches (aTime2, eDataType, eOperator, aTime0));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, aTime2));
    assertTrue (SearchMatcher.matches (aTime0, eDataType, eOperator, aTime2));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches (aTime2, eDataType, eOperator, "crap"));

    eOperator = ESearchOperator.LT;
    assertTrue (SearchMatcher.matches (aTime2, eDataType, eOperator, aTime1));
    assertFalse (SearchMatcher.matches (aTime2, eDataType, eOperator, aTime2));
    assertFalse (SearchMatcher.matches (aTime2, eDataType, eOperator, aTime3));
    assertTrue (SearchMatcher.matches (aTime2, eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches (aTime2, eDataType, eOperator, aTime0));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, aTime2));
    assertFalse (SearchMatcher.matches (aTime0, eDataType, eOperator, aTime2));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (aTime2, eDataType, eOperator, "crap"));

    eOperator = ESearchOperator.LE;
    assertTrue (SearchMatcher.matches (aTime2, eDataType, eOperator, aTime1));
    assertTrue (SearchMatcher.matches (aTime2, eDataType, eOperator, aTime2));
    assertFalse (SearchMatcher.matches (aTime2, eDataType, eOperator, aTime3));
    assertTrue (SearchMatcher.matches (aTime2, eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches (aTime2, eDataType, eOperator, aTime0));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, aTime2));
    assertFalse (SearchMatcher.matches (aTime0, eDataType, eOperator, aTime2));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (aTime2, eDataType, eOperator, "crap"));

    eOperator = ESearchOperator.GT;
    assertFalse (SearchMatcher.matches (aTime2, eDataType, eOperator, aTime1));
    assertFalse (SearchMatcher.matches (aTime2, eDataType, eOperator, aTime2));
    assertTrue (SearchMatcher.matches (aTime2, eDataType, eOperator, aTime3));
    assertFalse (SearchMatcher.matches (aTime2, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (aTime2, eDataType, eOperator, aTime0));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, aTime2));
    assertTrue (SearchMatcher.matches (aTime0, eDataType, eOperator, aTime2));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (aTime2, eDataType, eOperator, "crap"));

    eOperator = ESearchOperator.GE;
    assertFalse (SearchMatcher.matches (aTime2, eDataType, eOperator, aTime1));
    assertTrue (SearchMatcher.matches (aTime2, eDataType, eOperator, aTime2));
    assertTrue (SearchMatcher.matches (aTime2, eDataType, eOperator, aTime3));
    assertFalse (SearchMatcher.matches (aTime2, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (aTime2, eDataType, eOperator, aTime0));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, aTime2));
    assertTrue (SearchMatcher.matches (aTime0, eDataType, eOperator, aTime2));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (aTime2, eDataType, eOperator, "crap"));

    eOperator = ESearchOperator.EMPTY;
    assertFalse (SearchMatcher.matches (aTime2, eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (aTime0, eDataType, eOperator, null));

    eOperator = ESearchOperator.NOT_EMPTY;
    assertTrue (SearchMatcher.matches (aTime2, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches (aTime0, eDataType, eOperator, null));

    eOperator = ESearchOperator.TIME_HOUR;
    assertFalse (SearchMatcher.matches (aTime2, eDataType, eOperator, 10));
    assertTrue (SearchMatcher.matches (aTime2, eDataType, eOperator, 13));
    assertFalse (SearchMatcher.matches (aTime2, eDataType, eOperator, 16));
    assertFalse (SearchMatcher.matches (aTime2, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (aTime2, eDataType, eOperator, aTime0));
    assertFalse (SearchMatcher.matches (aTime2, eDataType, eOperator, "crap"));

    eOperator = ESearchOperator.TIME_MINUTE;
    assertFalse (SearchMatcher.matches (aTime2, eDataType, eOperator, 11));
    assertTrue (SearchMatcher.matches (aTime2, eDataType, eOperator, 14));
    assertFalse (SearchMatcher.matches (aTime2, eDataType, eOperator, 17));
    assertFalse (SearchMatcher.matches (aTime2, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (aTime2, eDataType, eOperator, aTime0));
    assertFalse (SearchMatcher.matches (aTime2, eDataType, eOperator, "crap"));

    eOperator = ESearchOperator.TIME_SECOND;
    assertFalse (SearchMatcher.matches (aTime2, eDataType, eOperator, 12));
    assertTrue (SearchMatcher.matches (aTime2, eDataType, eOperator, 15));
    assertFalse (SearchMatcher.matches (aTime2, eDataType, eOperator, 18));
    assertFalse (SearchMatcher.matches (aTime2, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (aTime2, eDataType, eOperator, aTime0));
    assertFalse (SearchMatcher.matches (aTime2, eDataType, eOperator, "crap"));
  }

  @Test
  public void testBoolean ()
  {
    final Boolean aBool1 = Boolean.TRUE;
    final Boolean aBool2 = Boolean.FALSE;
    final ESearchDataType eDataType = ESearchDataType.BOOLEAN;

    ESearchOperator eOperator = ESearchOperator.EQ;
    assertFalse (SearchMatcher.matches (aBool2, eDataType, eOperator, aBool1));
    assertTrue (SearchMatcher.matches (aBool2, eDataType, eOperator, aBool2));
    assertFalse (SearchMatcher.matches (aBool2, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, aBool2));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches (aBool2, eDataType, eOperator, "crap"));

    eOperator = ESearchOperator.NE;
    assertTrue (SearchMatcher.matches (aBool2, eDataType, eOperator, aBool1));
    assertFalse (SearchMatcher.matches (aBool2, eDataType, eOperator, aBool2));
    assertTrue (SearchMatcher.matches (aBool2, eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, aBool2));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (aBool2, eDataType, eOperator, "crap"));

    eOperator = ESearchOperator.EMPTY;
    assertFalse (SearchMatcher.matches (aBool2, eDataType, eOperator, null));
    assertTrue (SearchMatcher.matches (null, eDataType, eOperator, null));

    eOperator = ESearchOperator.NOT_EMPTY;
    assertTrue (SearchMatcher.matches (aBool2, eDataType, eOperator, null));
    assertFalse (SearchMatcher.matches (null, eDataType, eOperator, null));
  }
}
