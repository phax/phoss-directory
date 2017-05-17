package com.helger.pd.publisher.search;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

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
}
