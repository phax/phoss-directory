/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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
package com.helger.pd.indexer.mgr;

import static org.junit.Assert.assertNotNull;

import org.junit.Ignore;
import org.junit.Test;

import com.helger.commons.url.URLHelper;
import com.helger.pd.businesscard.PDExtendedBusinessCard;
import com.helger.peppol.identifier.participant.SimpleParticipantIdentifier;
import com.helger.peppol.smpclient.SMPClientReadOnly;

/**
 * Test class for class {@link SMPBusinessCardProvider}.
 *
 * @author Philip Helger
 */
public final class SMPBusinessCardProviderTest
{
  @Test
  @Ignore ("Does not work at the moment")
  public void testFetch ()
  {
    final SMPBusinessCardProvider aBI = new SMPBusinessCardProvider ();
    final PDExtendedBusinessCard aExtBI = aBI.getBusinessCard (SimpleParticipantIdentifier.createWithDefaultScheme ("9915:test"));
    assertNotNull (aExtBI);
    System.out.println (aExtBI);
  }

  @Test
  @Ignore ("Dont hammer server :)")
  public void test9905LeckmaPeppol ()
  {
    final SMPBusinessCardProvider aBI = new SMPBusinessCardProvider ();
    final PDExtendedBusinessCard aExtBI = aBI.getBusinessCard (SimpleParticipantIdentifier.createWithDefaultScheme ("9905:leckma-peppol"));
    assertNotNull (aExtBI);
    System.out.println (aExtBI);
  }

  @Test
  @Ignore ("Only for on demand testing :)")
  public void testFetchLocal ()
  {
    final SMPBusinessCardProvider aBI = new SMPBusinessCardProvider ();
    final PDExtendedBusinessCard aExtBI = aBI.getBusinessCard (SimpleParticipantIdentifier.createWithDefaultScheme ("9999:ghx"),
                                                               new SMPClientReadOnly (URLHelper.getAsURI ("http://localhost:90")));
    assertNotNull (aExtBI);
    System.out.println (aExtBI);
  }
}
