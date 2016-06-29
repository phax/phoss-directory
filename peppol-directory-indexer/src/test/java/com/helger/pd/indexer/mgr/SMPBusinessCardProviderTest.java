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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.url.URLHelper;
import com.helger.pd.businesscard.PDExtendedBusinessCard;
import com.helger.peppol.identifier.peppol.participant.PeppolParticipantIdentifier;
import com.helger.peppol.smpclient.SMPClientReadOnly;
import com.helger.web.scope.mock.WebScopeTestRule;

/**
 * Test class for class {@link SMPBusinessCardProvider}.
 *
 * @author Philip Helger
 */
public final class SMPBusinessCardProviderTest
{
  private static final Logger LOG = LoggerFactory.getLogger (SMPBusinessCardProviderTest.class);

  @Rule
  public final TestRule m_aRule = new WebScopeTestRule ();

  @Test
  public void testFetch ()
  {
    final SMPBusinessCardProvider aBI = new SMPBusinessCardProvider ();
    final PDExtendedBusinessCard aExtBI = aBI.getBusinessCard (PeppolParticipantIdentifier.createWithDefaultScheme ("9915:test"));
    assertNotNull (aExtBI);
    LOG.info (aExtBI.toString ());
  }

  @Test
  @Ignore ("Dont hammer server :)")
  public void test9905LeckmaPeppol ()
  {
    final SMPBusinessCardProvider aBI = new SMPBusinessCardProvider ();
    final PDExtendedBusinessCard aExtBI = aBI.getBusinessCard (PeppolParticipantIdentifier.createWithDefaultScheme ("9905:leckma-peppol"));
    assertNotNull (aExtBI);
    LOG.info (aExtBI.toString ());
  }

  @Test
  @Ignore ("Only for on demand testing :)")
  public void testFetchLocal ()
  {
    final SMPBusinessCardProvider aBI = new SMPBusinessCardProvider ();
    final PDExtendedBusinessCard aExtBI = aBI.getBusinessCard (PeppolParticipantIdentifier.createWithDefaultScheme ("9999:ghx"),
                                                               new SMPClientReadOnly (URLHelper.getAsURI ("http://localhost:90")));
    assertNotNull (aExtBI);
    LOG.info (aExtBI.toString ());
  }
}
