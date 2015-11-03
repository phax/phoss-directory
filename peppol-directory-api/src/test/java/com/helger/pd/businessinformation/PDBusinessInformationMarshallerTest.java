package com.helger.pd.businessinformation;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.helger.commons.io.resource.ClassPathResource;

/**
 * Test class for class {@link PDBusinessInformationMarshaller}.
 *
 * @author Philip Helger
 */
public final class PDBusinessInformationMarshallerTest
{
  @Test
  public void testBasic ()
  {
    final PDBusinessInformationMarshaller aMarshaller = new PDBusinessInformationMarshaller ();
    assertNotNull (aMarshaller.read (new ClassPathResource ("business-information-test1.xml")));
  }
}
