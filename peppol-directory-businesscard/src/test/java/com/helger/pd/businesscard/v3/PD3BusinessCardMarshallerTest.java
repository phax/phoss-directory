/**
 * Copyright (C) 2015-2019 Philip Helger (www.helger.com)
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
package com.helger.pd.businesscard.v3;

import static org.junit.Assert.assertNotNull;

import javax.annotation.Nonnull;

import org.junit.Test;

import com.helger.commons.io.resource.FileSystemResource;

/**
 * Test class for class {@link PD3BusinessCardMarshaller}.
 *
 * @author Philip Helger
 */
public final class PD3BusinessCardMarshallerTest
{
  private static void _testBC (@Nonnull final String sFilename)
  {
    final PD3BusinessCardMarshaller aMarshaller = new PD3BusinessCardMarshaller ();
    final PD3BusinessCardType aBC = aMarshaller.read (new FileSystemResource (sFilename));
    assertNotNull (aBC);
    assertNotNull (PD3APIHelper.createBusinessCard (aBC));
    assertNotNull (PD3APIHelper.createBusinessCard (aBC).getAsMicroXML ("urn:test", "bc"));
  }

  @Test
  public void testBasic ()
  {
    _testBC ("src/test/resources/example/v3/bc-0088-5033466000005.xml");
    _testBC ("src/test/resources/example/v3/bc-9915-leckma.xml");
    _testBC ("src/test/resources/example/v3/business-card-cctf-103.xml");
    _testBC ("src/test/resources/example/v3/business-card-example-spec.xml");
    _testBC ("src/test/resources/example/v3/business-card-test1.xml");
  }
}
