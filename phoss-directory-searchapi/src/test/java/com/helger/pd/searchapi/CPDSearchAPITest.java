package com.helger.pd.searchapi;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.commons.io.resource.ClassPathResource;

/**
 * Test class for class {@link CPDSearchAPI}.
 *
 * @author Philip Helger
 */
public final class CPDSearchAPITest
{
  @Test
  public void testBasic ()
  {
    for (final ClassPathResource aCPR : CPDSearchAPI.RESULT_LIST_V1_XSDS)
      assertTrue ("Missing: " + aCPR.getPath (), aCPR.exists ());
  }
}
