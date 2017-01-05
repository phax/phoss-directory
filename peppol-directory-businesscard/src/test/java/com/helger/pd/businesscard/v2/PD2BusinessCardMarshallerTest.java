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
package com.helger.pd.businesscard.v2;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.helger.commons.io.resource.FileSystemResource;

/**
 * Test class for class {@link PD2BusinessCardMarshaller}.
 *
 * @author Philip Helger
 */
public final class PD2BusinessCardMarshallerTest
{
  @Test
  public void testBasic ()
  {
    final PD2BusinessCardMarshaller aMarshaller = new PD2BusinessCardMarshaller ();
    assertNotNull (aMarshaller.read (new FileSystemResource ("src/test/resources/example/v2/business-card-test1.xml")));
    assertNotNull (aMarshaller.read (new FileSystemResource ("src/test/resources/example/v2/business-card-example-spec.xml")));
    assertNotNull (aMarshaller.read (new FileSystemResource ("src/test/resources/example/v2/bc-9915-leckma.xml")));
    assertNotNull (aMarshaller.read (new FileSystemResource ("src/test/resources/example/v2/bc-0088-5033466000005.xml")));
  }
}
