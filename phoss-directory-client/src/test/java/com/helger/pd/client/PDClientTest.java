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
package com.helger.pd.client;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.exception.InitializationException;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.peppol.identifier.factory.PeppolIdentifierFactory;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;

/**
 * Test class for class {@link PDClient}.
 *
 * @author Philip Helger
 */
public final class PDClientTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PDClientTest.class);

  @Test
  @Ignore
  public void testNonExistingPort ()
  {
    final IParticipantIdentifier aPI = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:test");
    try (final PDClient aClient = new PDClient ("http://193.10.8.211:7999"))
    {
      if (aClient.deleteServiceGroupFromIndex (aPI).isSuccess ())
      {
        aClient.isServiceGroupRegistered (aPI);
        aClient.addServiceGroupToIndex (aPI);
      }
    }
    catch (final InitializationException ex)
    {
      LOGGER.error ("Failed to invoke PDClient", ex);
    }
  }

  @Test
  public void testTestServer ()
  {
    final IParticipantIdentifier aPI = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:test");
    try (final PDClient aClient = new PDClient ("https://test-directory.peppol.eu"))
    {
      if (aClient.deleteServiceGroupFromIndex (aPI).isSuccess ())
      {
        aClient.isServiceGroupRegistered (aPI);
        aClient.addServiceGroupToIndex (aPI);
      }
    }
    catch (final InitializationException ex)
    {
      LOGGER.error ("Failed to invoke PDClient", ex);
    }
  }

  @Test
  @Ignore ("Test manually only!")
  public void testProductionServer ()
  {
    System.setProperty (PDClientConfiguration.SYSTEM_PROPERTY_PRIMARY,
                        ClassPathResource.getAsFile ("private-pd-client-prod.properties").getAbsolutePath ());

    final IParticipantIdentifier aPI = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:b");
    try (final PDClient aClient = new PDClient ("https://directory.peppol.eu"))
    {
      if (aClient.deleteServiceGroupFromIndex (aPI).isSuccess ())
      {
        aClient.isServiceGroupRegistered (aPI);
        aClient.addServiceGroupToIndex (aPI);
      }
    }
    catch (final InitializationException ex)
    {
      LOGGER.error ("Failed to invoke PDClient", ex);
    }
    finally
    {
      System.clearProperty (PDClientConfiguration.SYSTEM_PROPERTY_PRIMARY);
    }
  }
}
