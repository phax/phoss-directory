package com.helger.pd.publisher.search;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.junit.Test;

/**
 * Test class for class {@link EPDMatchType}.
 *
 * @author Philip Helger
 */
public final class EPDMatchTypeTest
{
  @Test
  public void testBasic ()
  {
    for (final EPDMatchType e : EPDMatchType.values ())
    {
      assertSame (e, EPDMatchType.getFromIDOrNull (e.getID ()));
    }
  }

  @Test
  public void testMatch ()
  {
    final String s1 = "Stored Value";
    final String s2 = "Stored";
    final String s3 = "ed Va";
    for (final EPDMatchType e : EPDMatchType.values ())
    {
      assertTrue (e.matches (s1, s1));
      assertTrue (e.matches (s2, s2));
      assertFalse (e.matches (s2, s3));
    }

    EPDMatchType e = EPDMatchType.EXACT_MATCH_CS;
    assertTrue (e.matches (s1, s1));
    assertFalse (e.matches (s1, s1.toUpperCase (Locale.US)));
    assertFalse (e.matches (s1, s2));
    assertFalse (e.matches (s1, s2.toUpperCase (Locale.US)));
    assertFalse (e.matches (s1, s3));
    assertFalse (e.matches (s1, s3.toUpperCase (Locale.US)));

    e = EPDMatchType.EXACT_MATCH_CI;
    assertTrue (e.matches (s1, s1));
    assertTrue (e.matches (s1, s1.toUpperCase (Locale.US)));
    assertFalse (e.matches (s1, s2));
    assertFalse (e.matches (s1, s2.toUpperCase (Locale.US)));
    assertFalse (e.matches (s1, s3));
    assertFalse (e.matches (s1, s3.toUpperCase (Locale.US)));

    e = EPDMatchType.PARTIAL_MATCH_CI;
    assertTrue (e.matches (s1, s1));
    assertTrue (e.matches (s1, s1.toUpperCase (Locale.US)));
    assertTrue (e.matches (s1, s2));
    assertTrue (e.matches (s1, s2.toUpperCase (Locale.US)));
    assertTrue (e.matches (s1, s3));
    assertTrue (e.matches (s1, s3.toUpperCase (Locale.US)));

    e = EPDMatchType.STARTSWITH_MATCH_CI;
    assertTrue (e.matches (s1, s1));
    assertTrue (e.matches (s1, s1.toUpperCase (Locale.US)));
    assertTrue (e.matches (s1, s2));
    assertTrue (e.matches (s1, s2.toUpperCase (Locale.US)));
    assertFalse (e.matches (s1, s3));
    assertFalse (e.matches (s1, s3.toUpperCase (Locale.US)));
  }
}
