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
 * Test class for class {@link PDMatcher}.
 *
 * @author Philip Helger
 */
public final class PDMatcherTest
{
  @Test
  public void testStringCS ()
  {
    final EPDSearchDataType eDataType = EPDSearchDataType.STRING_CS;

    EPDSearchOperator eOperator = EPDSearchOperator.EQ;
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, "bl"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "bla"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, "la"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, ""));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, "bla"));
    assertFalse (PDMatcher.matches ("", eDataType, eOperator, "bla"));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.NE;
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "bl"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, "bla"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "la"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, null));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, ""));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, "bla"));
    assertTrue (PDMatcher.matches ("", eDataType, eOperator, "bla"));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.LT;
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "bl"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, "bla"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, "la"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, null));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, ""));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, "bla"));
    assertFalse (PDMatcher.matches ("", eDataType, eOperator, "bla"));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.LE;
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "bl"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "bla"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, "la"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, null));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, ""));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, "bla"));
    assertFalse (PDMatcher.matches ("", eDataType, eOperator, "bla"));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.GT;
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, "bl"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, "bla"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "la"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, ""));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, "bla"));
    assertTrue (PDMatcher.matches ("", eDataType, eOperator, "bla"));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.GE;
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, "bl"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "bla"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "la"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, ""));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, "bla"));
    assertTrue (PDMatcher.matches ("", eDataType, eOperator, "bla"));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.EMPTY;
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, null));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, null));
    assertTrue (PDMatcher.matches ("", eDataType, eOperator, null));

    eOperator = EPDSearchOperator.NOT_EMPTY;
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches ("", eDataType, eOperator, null));

    eOperator = EPDSearchOperator.STRING_CONTAINS;
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "bl"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "bla"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "la"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, ""));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, "bla"));
    assertFalse (PDMatcher.matches ("", eDataType, eOperator, "bla"));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.STRING_STARTS_WITH;
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "bl"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "bla"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, "la"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, ""));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, "bla"));
    assertFalse (PDMatcher.matches ("", eDataType, eOperator, "bla"));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.STRING_ENDS_WITH;
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, "bl"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "bla"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "la"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, ""));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, "bla"));
    assertFalse (PDMatcher.matches ("", eDataType, eOperator, "bla"));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.STRING_REGEX;
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, "bl"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "bla"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, "la"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, ""));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, "bla"));
    assertFalse (PDMatcher.matches ("", eDataType, eOperator, "bla"));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, ".*la?"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, "[0-9]*"));
  }

  @Test
  public void testStringCI_CS ()
  {
    final EPDSearchDataType eDataType = EPDSearchDataType.STRING_CI;

    EPDSearchOperator eOperator = EPDSearchOperator.EQ;
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, "bl"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "bla"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, "la"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, ""));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, "bla"));
    assertFalse (PDMatcher.matches ("", eDataType, eOperator, "bla"));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.NE;
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "bl"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, "bla"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "la"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, null));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, ""));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, "bla"));
    assertTrue (PDMatcher.matches ("", eDataType, eOperator, "bla"));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.LT;
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "bl"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, "bla"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, "la"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, null));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, ""));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, "bla"));
    assertFalse (PDMatcher.matches ("", eDataType, eOperator, "bla"));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.LE;
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "bl"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "bla"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, "la"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, null));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, ""));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, "bla"));
    assertFalse (PDMatcher.matches ("", eDataType, eOperator, "bla"));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.GT;
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, "bl"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, "bla"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "la"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, ""));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, "bla"));
    assertTrue (PDMatcher.matches ("", eDataType, eOperator, "bla"));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.GE;
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, "bl"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "bla"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "la"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, ""));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, "bla"));
    assertTrue (PDMatcher.matches ("", eDataType, eOperator, "bla"));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.EMPTY;
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, null));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, null));
    assertTrue (PDMatcher.matches ("", eDataType, eOperator, null));

    eOperator = EPDSearchOperator.NOT_EMPTY;
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches ("", eDataType, eOperator, null));

    eOperator = EPDSearchOperator.STRING_CONTAINS;
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "bl"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "bla"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "la"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, ""));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, "bla"));
    assertFalse (PDMatcher.matches ("", eDataType, eOperator, "bla"));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.STRING_STARTS_WITH;
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "bl"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "bla"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, "la"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, ""));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, "bla"));
    assertFalse (PDMatcher.matches ("", eDataType, eOperator, "bla"));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.STRING_ENDS_WITH;
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, "bl"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "bla"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "la"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, ""));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, "bla"));
    assertFalse (PDMatcher.matches ("", eDataType, eOperator, "bla"));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.STRING_REGEX;
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, "bl"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "bla"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, "la"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, ""));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, "bla"));
    assertFalse (PDMatcher.matches ("", eDataType, eOperator, "bla"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, ".*la?"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, "[0-9]*"));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));
  }

  @Test
  public void testStringCI_CI ()
  {
    final EPDSearchDataType eDataType = EPDSearchDataType.STRING_CI;

    EPDSearchOperator eOperator = EPDSearchOperator.EQ;
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, "BL"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "BLA"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, "LA"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, ""));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, "BLA"));
    assertFalse (PDMatcher.matches ("", eDataType, eOperator, "BLA"));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.NE;
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "BL"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, "BLA"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "LA"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, null));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, ""));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, "BLA"));
    assertTrue (PDMatcher.matches ("", eDataType, eOperator, "BLA"));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.LT;
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "BL"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, "BLA"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, "LA"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, null));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, ""));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, "BLA"));
    assertFalse (PDMatcher.matches ("", eDataType, eOperator, "BLA"));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.LE;
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "BL"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "BLA"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, "LA"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, null));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, ""));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, "BLA"));
    assertFalse (PDMatcher.matches ("", eDataType, eOperator, "BLA"));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.GT;
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, "BL"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, "BLA"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "LA"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, ""));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, "BLA"));
    assertTrue (PDMatcher.matches ("", eDataType, eOperator, "BLA"));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.GE;
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, "BL"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "BLA"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "LA"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, ""));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, "BLA"));
    assertTrue (PDMatcher.matches ("", eDataType, eOperator, "BLA"));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.EMPTY;
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, null));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, null));
    assertTrue (PDMatcher.matches ("", eDataType, eOperator, null));

    eOperator = EPDSearchOperator.NOT_EMPTY;
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches ("", eDataType, eOperator, null));

    eOperator = EPDSearchOperator.STRING_CONTAINS;
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "BL"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "BLA"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "LA"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, ""));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, "BLA"));
    assertFalse (PDMatcher.matches ("", eDataType, eOperator, "BLA"));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.STRING_STARTS_WITH;
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "BL"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "BLA"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, "LA"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, ""));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, "BLA"));
    assertFalse (PDMatcher.matches ("", eDataType, eOperator, "BLA"));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.STRING_ENDS_WITH;
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, "BL"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "BLA"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "LA"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, ""));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, "BLA"));
    assertFalse (PDMatcher.matches ("", eDataType, eOperator, "BLA"));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.STRING_REGEX;
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, "BL"));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, "BLA"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, "LA"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, null));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, ""));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, "BLA"));
    assertFalse (PDMatcher.matches ("", eDataType, eOperator, "BLA"));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));
    assertTrue (PDMatcher.matches ("bla", eDataType, eOperator, ".*LA?"));
    assertFalse (PDMatcher.matches ("bla", eDataType, eOperator, "[0-9]*"));
  }

  @SuppressWarnings ("boxing")
  @Test
  public void testLong ()
  {
    final EPDSearchDataType eDataType = EPDSearchDataType.INT;

    EPDSearchOperator eOperator = EPDSearchOperator.EQ;
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, 1));
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, 123));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, 23));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, 0));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, 123));
    assertFalse (PDMatcher.matches (0, eDataType, eOperator, 123));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.NE;
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, 1));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, 123));
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, 23));
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, null));
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, 0));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, 123));
    assertTrue (PDMatcher.matches (0, eDataType, eOperator, 123));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.LT;
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, 1));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, 123));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, 234));
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, null));
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, 0));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, 123));
    assertFalse (PDMatcher.matches (0, eDataType, eOperator, 123));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.LE;
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, 1));
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, 123));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, 234));
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, null));
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, 0));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, 123));
    assertFalse (PDMatcher.matches (0, eDataType, eOperator, 123));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.GT;
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, 1));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, 123));
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, 234));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, 0));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, 123));
    assertTrue (PDMatcher.matches (0, eDataType, eOperator, 123));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.GE;
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, 1));
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, 123));
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, 234));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, 0));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, 123));
    assertTrue (PDMatcher.matches (0, eDataType, eOperator, 123));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.EMPTY;
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, null));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (0, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.NOT_EMPTY;
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));
    assertTrue (PDMatcher.matches (0, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.INT_EVEN;
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));
    assertTrue (PDMatcher.matches (0, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.INT_ODD;
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (0, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.STRING_CONTAINS;
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, 1));
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, 123));
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, 23));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, 0));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, 123));
    assertFalse (PDMatcher.matches (0, eDataType, eOperator, 123));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.STRING_STARTS_WITH;
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, 1));
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, 123));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, 23));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, 0));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, 123));
    assertFalse (PDMatcher.matches (0, eDataType, eOperator, 123));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.STRING_ENDS_WITH;
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, 1));
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, 123));
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, 23));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, 0));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, 123));
    assertFalse (PDMatcher.matches (0, eDataType, eOperator, 123));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.STRING_REGEX;
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, 1));
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, 123));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, 23));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, 0));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, 123));
    assertFalse (PDMatcher.matches (0, eDataType, eOperator, 123));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, "[1-3]+"));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, ".*4.*"));
  }

  @SuppressWarnings ("boxing")
  @Test
  public void testDouble ()
  {
    final EPDSearchDataType eDataType = EPDSearchDataType.DOUBLE;

    EPDSearchOperator eOperator = EPDSearchOperator.EQ;
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, 1));
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, 123));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, 23));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, 0));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, 123));
    assertFalse (PDMatcher.matches (0, eDataType, eOperator, 123));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, null));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.NE;
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, 1));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, 123));
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, 23));
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, null));
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, 0));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, 123));
    assertTrue (PDMatcher.matches (0, eDataType, eOperator, 123));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.LT;
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, 1));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, 123));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, 234));
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, null));
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, 0));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, 123));
    assertFalse (PDMatcher.matches (0, eDataType, eOperator, 123));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.LE;
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, 1));
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, 123));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, 234));
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, null));
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, 0));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, 123));
    assertFalse (PDMatcher.matches (0, eDataType, eOperator, 123));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.GT;
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, 1));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, 123));
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, 234));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, 0));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, 123));
    assertTrue (PDMatcher.matches (0, eDataType, eOperator, 123));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.GE;
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, 1));
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, 123));
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, 234));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, 0));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, 123));
    assertTrue (PDMatcher.matches (0, eDataType, eOperator, 123));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.EMPTY;
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, null));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (0, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.NOT_EMPTY;
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));
    assertTrue (PDMatcher.matches (0, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.STRING_CONTAINS;
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, 1));
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, 123));
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, 23));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, 0));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, 123));
    assertFalse (PDMatcher.matches (0, eDataType, eOperator, 123));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.STRING_STARTS_WITH;
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, 1));
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, 123));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, 23));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, 0));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, 123));
    assertFalse (PDMatcher.matches (0, eDataType, eOperator, 123));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.STRING_ENDS_WITH;
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, 1));
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, 123));
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, 23));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, 0));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, 123));
    assertFalse (PDMatcher.matches (0, eDataType, eOperator, 123));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.STRING_REGEX;
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, 1));
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, 123));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, 23));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, 0));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, 123));
    assertFalse (PDMatcher.matches (0, eDataType, eOperator, 123));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));
    assertTrue (PDMatcher.matches (123, eDataType, eOperator, "[1-3]+"));
    assertFalse (PDMatcher.matches (123, eDataType, eOperator, ".*4.*"));
  }

  @SuppressWarnings ("boxing")
  @Test
  public void testDate ()
  {
    final LocalDate aDate0 = PDTFactory.createLocalDate (0);
    final LocalDate aDate1 = PDTFactory.createLocalDate (2017, Month.FEBRUARY, 2);
    final LocalDate aDate2 = PDTFactory.createLocalDate (2017, Month.MARCH, 3);
    final LocalDate aDate3 = PDTFactory.createLocalDate (2017, Month.APRIL, 4);
    final EPDSearchDataType eDataType = EPDSearchDataType.DATE;

    EPDSearchOperator eOperator = EPDSearchOperator.EQ;
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, aDate1));
    assertTrue (PDMatcher.matches (aDate2, eDataType, eOperator, aDate2));
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, aDate3));
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, aDate0));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, aDate2));
    assertFalse (PDMatcher.matches (aDate0, eDataType, eOperator, aDate2));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, null));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, "crap"));

    eOperator = EPDSearchOperator.NE;
    assertTrue (PDMatcher.matches (aDate2, eDataType, eOperator, aDate1));
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, aDate2));
    assertTrue (PDMatcher.matches (aDate2, eDataType, eOperator, aDate3));
    assertTrue (PDMatcher.matches (aDate2, eDataType, eOperator, null));
    assertTrue (PDMatcher.matches (aDate2, eDataType, eOperator, aDate0));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, aDate2));
    assertTrue (PDMatcher.matches (aDate0, eDataType, eOperator, aDate2));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));
    assertTrue (PDMatcher.matches (aDate2, eDataType, eOperator, "crap"));

    eOperator = EPDSearchOperator.LT;
    assertTrue (PDMatcher.matches (aDate2, eDataType, eOperator, aDate1));
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, aDate2));
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, aDate3));
    assertTrue (PDMatcher.matches (aDate2, eDataType, eOperator, null));
    assertTrue (PDMatcher.matches (aDate2, eDataType, eOperator, aDate0));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, aDate2));
    assertFalse (PDMatcher.matches (aDate0, eDataType, eOperator, aDate2));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, "crap"));

    eOperator = EPDSearchOperator.LE;
    assertTrue (PDMatcher.matches (aDate2, eDataType, eOperator, aDate1));
    assertTrue (PDMatcher.matches (aDate2, eDataType, eOperator, aDate2));
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, aDate3));
    assertTrue (PDMatcher.matches (aDate2, eDataType, eOperator, null));
    assertTrue (PDMatcher.matches (aDate2, eDataType, eOperator, aDate0));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, aDate2));
    assertFalse (PDMatcher.matches (aDate0, eDataType, eOperator, aDate2));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, "crap"));

    eOperator = EPDSearchOperator.GT;
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, aDate1));
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, aDate2));
    assertTrue (PDMatcher.matches (aDate2, eDataType, eOperator, aDate3));
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, aDate0));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, aDate2));
    assertTrue (PDMatcher.matches (aDate0, eDataType, eOperator, aDate2));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, "crap"));

    eOperator = EPDSearchOperator.GE;
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, aDate1));
    assertTrue (PDMatcher.matches (aDate2, eDataType, eOperator, aDate2));
    assertTrue (PDMatcher.matches (aDate2, eDataType, eOperator, aDate3));
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, aDate0));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, aDate2));
    assertTrue (PDMatcher.matches (aDate0, eDataType, eOperator, aDate2));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, "crap"));

    eOperator = EPDSearchOperator.EMPTY;
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, null));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (aDate0, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.NOT_EMPTY;
    assertTrue (PDMatcher.matches (aDate2, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));
    assertTrue (PDMatcher.matches (aDate0, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.DATE_YEAR;
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, 2016));
    assertTrue (PDMatcher.matches (aDate2, eDataType, eOperator, 2017));
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, 2018));
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, aDate0));
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, "crap"));

    eOperator = EPDSearchOperator.DATE_MONTH;
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, Month.JANUARY));
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, 2));
    assertTrue (PDMatcher.matches (aDate2, eDataType, eOperator, Month.MARCH));
    assertTrue (PDMatcher.matches (aDate2, eDataType, eOperator, 3));
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, 4));
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, Month.DECEMBER));
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, aDate0));
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, "crap"));

    eOperator = EPDSearchOperator.DATE_DAY;
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, 2));
    assertTrue (PDMatcher.matches (aDate2, eDataType, eOperator, 3));
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, 4));
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, aDate0));
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, "crap"));

    eOperator = EPDSearchOperator.DATE_YEAR_MONTH;
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, YearMonth.of (2017, Month.FEBRUARY)));
    assertTrue (PDMatcher.matches (aDate2, eDataType, eOperator, YearMonth.of (2017, Month.MARCH)));
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, YearMonth.of (2017, Month.APRIL)));
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, 4));
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, aDate0));
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, "crap"));

    eOperator = EPDSearchOperator.DATE_MONTH_DAY;
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, MonthDay.of (Month.FEBRUARY, 2)));
    assertTrue (PDMatcher.matches (aDate2, eDataType, eOperator, MonthDay.of (Month.MARCH, 3)));
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, MonthDay.of (Month.APRIL, 4)));
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, 4));
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, aDate0));
    assertFalse (PDMatcher.matches (aDate2, eDataType, eOperator, "crap"));
  }

  @SuppressWarnings ("boxing")
  @Test
  public void testTime ()
  {
    final LocalTime aTime0 = CPDT.NULL_LOCAL_TIME;
    final LocalTime aTime1 = PDTFactory.createLocalTime (10, 11, 12);
    final LocalTime aTime2 = PDTFactory.createLocalTime (13, 14, 15);
    final LocalTime aTime3 = PDTFactory.createLocalTime (16, 17, 18);
    final EPDSearchDataType eDataType = EPDSearchDataType.TIME;

    EPDSearchOperator eOperator = EPDSearchOperator.EQ;
    assertFalse (PDMatcher.matches (aTime2, eDataType, eOperator, aTime1));
    assertTrue (PDMatcher.matches (aTime2, eDataType, eOperator, aTime2));
    assertFalse (PDMatcher.matches (aTime2, eDataType, eOperator, aTime3));
    assertFalse (PDMatcher.matches (aTime2, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (aTime2, eDataType, eOperator, aTime0));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, aTime2));
    assertFalse (PDMatcher.matches (aTime0, eDataType, eOperator, aTime2));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, null));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (aTime2, eDataType, eOperator, "crap"));

    eOperator = EPDSearchOperator.NE;
    assertTrue (PDMatcher.matches (aTime2, eDataType, eOperator, aTime1));
    assertFalse (PDMatcher.matches (aTime2, eDataType, eOperator, aTime2));
    assertTrue (PDMatcher.matches (aTime2, eDataType, eOperator, aTime3));
    assertTrue (PDMatcher.matches (aTime2, eDataType, eOperator, null));
    assertTrue (PDMatcher.matches (aTime2, eDataType, eOperator, aTime0));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, aTime2));
    assertTrue (PDMatcher.matches (aTime0, eDataType, eOperator, aTime2));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));
    assertTrue (PDMatcher.matches (aTime2, eDataType, eOperator, "crap"));

    eOperator = EPDSearchOperator.LT;
    assertTrue (PDMatcher.matches (aTime2, eDataType, eOperator, aTime1));
    assertFalse (PDMatcher.matches (aTime2, eDataType, eOperator, aTime2));
    assertFalse (PDMatcher.matches (aTime2, eDataType, eOperator, aTime3));
    assertTrue (PDMatcher.matches (aTime2, eDataType, eOperator, null));
    assertTrue (PDMatcher.matches (aTime2, eDataType, eOperator, aTime0));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, aTime2));
    assertFalse (PDMatcher.matches (aTime0, eDataType, eOperator, aTime2));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (aTime2, eDataType, eOperator, "crap"));

    eOperator = EPDSearchOperator.LE;
    assertTrue (PDMatcher.matches (aTime2, eDataType, eOperator, aTime1));
    assertTrue (PDMatcher.matches (aTime2, eDataType, eOperator, aTime2));
    assertFalse (PDMatcher.matches (aTime2, eDataType, eOperator, aTime3));
    assertTrue (PDMatcher.matches (aTime2, eDataType, eOperator, null));
    assertTrue (PDMatcher.matches (aTime2, eDataType, eOperator, aTime0));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, aTime2));
    assertFalse (PDMatcher.matches (aTime0, eDataType, eOperator, aTime2));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (aTime2, eDataType, eOperator, "crap"));

    eOperator = EPDSearchOperator.GT;
    assertFalse (PDMatcher.matches (aTime2, eDataType, eOperator, aTime1));
    assertFalse (PDMatcher.matches (aTime2, eDataType, eOperator, aTime2));
    assertTrue (PDMatcher.matches (aTime2, eDataType, eOperator, aTime3));
    assertFalse (PDMatcher.matches (aTime2, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (aTime2, eDataType, eOperator, aTime0));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, aTime2));
    assertTrue (PDMatcher.matches (aTime0, eDataType, eOperator, aTime2));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (aTime2, eDataType, eOperator, "crap"));

    eOperator = EPDSearchOperator.GE;
    assertFalse (PDMatcher.matches (aTime2, eDataType, eOperator, aTime1));
    assertTrue (PDMatcher.matches (aTime2, eDataType, eOperator, aTime2));
    assertTrue (PDMatcher.matches (aTime2, eDataType, eOperator, aTime3));
    assertFalse (PDMatcher.matches (aTime2, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (aTime2, eDataType, eOperator, aTime0));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, aTime2));
    assertTrue (PDMatcher.matches (aTime0, eDataType, eOperator, aTime2));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (aTime2, eDataType, eOperator, "crap"));

    eOperator = EPDSearchOperator.EMPTY;
    assertFalse (PDMatcher.matches (aTime2, eDataType, eOperator, null));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (aTime0, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.NOT_EMPTY;
    assertTrue (PDMatcher.matches (aTime2, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));
    assertTrue (PDMatcher.matches (aTime0, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.TIME_HOUR;
    assertFalse (PDMatcher.matches (aTime2, eDataType, eOperator, 10));
    assertTrue (PDMatcher.matches (aTime2, eDataType, eOperator, 13));
    assertFalse (PDMatcher.matches (aTime2, eDataType, eOperator, 16));
    assertFalse (PDMatcher.matches (aTime2, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (aTime2, eDataType, eOperator, aTime0));
    assertFalse (PDMatcher.matches (aTime2, eDataType, eOperator, "crap"));

    eOperator = EPDSearchOperator.TIME_MINUTE;
    assertFalse (PDMatcher.matches (aTime2, eDataType, eOperator, 11));
    assertTrue (PDMatcher.matches (aTime2, eDataType, eOperator, 14));
    assertFalse (PDMatcher.matches (aTime2, eDataType, eOperator, 17));
    assertFalse (PDMatcher.matches (aTime2, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (aTime2, eDataType, eOperator, aTime0));
    assertFalse (PDMatcher.matches (aTime2, eDataType, eOperator, "crap"));

    eOperator = EPDSearchOperator.TIME_SECOND;
    assertFalse (PDMatcher.matches (aTime2, eDataType, eOperator, 12));
    assertTrue (PDMatcher.matches (aTime2, eDataType, eOperator, 15));
    assertFalse (PDMatcher.matches (aTime2, eDataType, eOperator, 18));
    assertFalse (PDMatcher.matches (aTime2, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (aTime2, eDataType, eOperator, aTime0));
    assertFalse (PDMatcher.matches (aTime2, eDataType, eOperator, "crap"));
  }

  @Test
  public void testBoolean ()
  {
    final Boolean aBool1 = Boolean.TRUE;
    final Boolean aBool2 = Boolean.FALSE;
    final EPDSearchDataType eDataType = EPDSearchDataType.BOOLEAN;

    EPDSearchOperator eOperator = EPDSearchOperator.EQ;
    assertFalse (PDMatcher.matches (aBool2, eDataType, eOperator, aBool1));
    assertTrue (PDMatcher.matches (aBool2, eDataType, eOperator, aBool2));
    assertFalse (PDMatcher.matches (aBool2, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, aBool2));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, null));
    assertTrue (PDMatcher.matches (aBool2, eDataType, eOperator, "crap"));

    eOperator = EPDSearchOperator.NE;
    assertTrue (PDMatcher.matches (aBool2, eDataType, eOperator, aBool1));
    assertFalse (PDMatcher.matches (aBool2, eDataType, eOperator, aBool2));
    assertTrue (PDMatcher.matches (aBool2, eDataType, eOperator, null));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, aBool2));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (aBool2, eDataType, eOperator, "crap"));

    eOperator = EPDSearchOperator.EMPTY;
    assertFalse (PDMatcher.matches (aBool2, eDataType, eOperator, null));
    assertTrue (PDMatcher.matches (null, eDataType, eOperator, null));

    eOperator = EPDSearchOperator.NOT_EMPTY;
    assertTrue (PDMatcher.matches (aBool2, eDataType, eOperator, null));
    assertFalse (PDMatcher.matches (null, eDataType, eOperator, null));
  }
}
