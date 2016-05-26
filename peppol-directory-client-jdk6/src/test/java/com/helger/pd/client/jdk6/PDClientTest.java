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
package com.helger.pd.client.jdk6;

import org.junit.Test;

import com.helger.commons.exception.InitializationException;
import com.helger.pd.client.jdk6.PDClient;
import com.helger.pd.client.jdk6.PDClientConfiguration;
import com.helger.peppol.identifier.participant.SimpleParticipantIdentifier;

/**
 * Test class for class {@link PDClient}.
 *
 * @author Philip Helger
 */
public final class PDClientTest
{
  static
  {
    PDClientConfiguration.getConfigFile ().applyAllNetworkSystemProperties ();
  }

  @Test
  public void testBasic ()
  {
    final SimpleParticipantIdentifier aPI = SimpleParticipantIdentifier.createWithDefaultScheme ("9999:client-test");
    final PDClient aClient = PDClient.createDefaultClient ();
    try
    {
      aClient.deleteServiceGroupFromIndex (aPI);
      aClient.isServiceGroupRegistered (aPI);
      aClient.addServiceGroupToIndex (aPI);
    }
    catch (final InitializationException ex)
    {
      ex.printStackTrace ();
    }
  }
}
