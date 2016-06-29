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
package com.helger.pd.client;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.exception.InitializationException;
import com.helger.peppol.identifier.peppol.participant.PeppolParticipantIdentifier;

/**
 * Test class for class {@link PDClient}.
 *
 * @author Philip Helger
 */
public final class PDClientTest
{
  private static final Logger LOG = LoggerFactory.getLogger (PDClientTest.class);

  @Test
  public void testBasic ()
  {
    final PeppolParticipantIdentifier aPI = PeppolParticipantIdentifier.createWithDefaultScheme ("9999:client-test");
    try (final PDClient aClient = PDClient.createDefaultClient ())
    {
      aClient.deleteServiceGroupFromIndex (aPI);
      aClient.isServiceGroupRegistered (aPI);
      aClient.addServiceGroupToIndex (aPI);
    }
    catch (final InitializationException ex)
    {
      LOG.error ("Failed to invoke PDClient", ex);
    }
  }
}
