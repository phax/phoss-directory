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
package com.helger.pd.indexer.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.datetime.helper.PDTFactory;

public final class PDStoredMetaDataTest
{
  @Test
  public void testMillis ()
  {
    final long nMillis = System.currentTimeMillis ();
    final long nMillis2 = PDTFactory.getMillis (new PDStoredMetaData (PDTFactory.getCurrentLocalDateTime (),
                                                                      "me",
                                                                      "localhost").getCreationDT ());
    // Less than a second difference
    assertTrue (nMillis + " vs. " + nMillis2, Math.abs (nMillis2 - nMillis) <= 1000);
  }

  @Test
  public void testGetOwnerIDSeatNumber ()
  {
    assertEquals ("000155", PDStoredMetaData.getOwnerIDSeatNumber ("CN=PSG000155,O=SGNIC,C=SG"));
    assertEquals ("000155", PDStoredMetaData.getOwnerIDSeatNumber ("C=SG,CN=PSG000155,O=SGNIC"));
    assertEquals ("000155", PDStoredMetaData.getOwnerIDSeatNumber ("O=SGNIC,C=SG,CN=PSG000155"));
    assertEquals ("000155", PDStoredMetaData.getOwnerIDSeatNumber ("CN=PSG000155"));
    assertNull (PDStoredMetaData.getOwnerIDSeatNumber ("O=SGNIC,C=SG,N=PSG000155"));
    assertNull (PDStoredMetaData.getOwnerIDSeatNumber ("O=SGNIC,C=SG,CN=PSG00015"));
    assertNull (PDStoredMetaData.getOwnerIDSeatNumber ("CN=PSG00015,O=SGNIC,C=SG"));
    assertNull (PDStoredMetaData.getOwnerIDSeatNumber ("CN=PS000155,O=SGNIC,C=SG"));
    assertNull (PDStoredMetaData.getOwnerIDSeatNumber ("CN=QSG000155,O=SGNIC,C=SG"));
  }
}
