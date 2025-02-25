/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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
package com.helger.pd.indexer.settings;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.helger.commons.collection.impl.CommonsHashSet;
import com.helger.commons.collection.impl.ICommonsList;

/**
 * Test class for class {@link PDServerConfiguration}.
 *
 * @author Philip Helger
 */
public final class PDServerConfigurationTest
{
  @Test
  public void testClientCerts ()
  {
    final ICommonsList <String> aList = PDServerConfiguration.getAllClientCertIssuer ();
    assertEquals (1, aList.size ());
    // Check if all items are unique
    assertEquals (1, new CommonsHashSet <> (aList).size ());
    assertEquals ("CN=PEPPOL SERVICE METADATA PUBLISHER TEST CA - G2,OU=FOR TEST ONLY,O=OpenPEPPOL AISBL,C=BE",
                  aList.get (0));
    if (false)
      assertEquals ("CN=PEPPOL SERVICE METADATA PUBLISHER CA - G2,O=OpenPEPPOL AISBL,C=BE", aList.get (0));
  }

  @Test
  public void testTrustStores ()
  {
    final ICommonsList <PDConfiguredTrustStore> aList = PDServerConfiguration.getAllTrustStores ();
    assertEquals (1, aList.size ());
  }
}
