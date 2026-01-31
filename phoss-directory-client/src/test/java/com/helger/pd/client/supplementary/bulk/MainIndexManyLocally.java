/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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
package com.helger.pd.client.supplementary.bulk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.exception.InitializationException;
import com.helger.base.string.StringHelper;
import com.helger.pd.client.PDClient;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;

public class MainIndexManyLocally
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainIndexManyLocally.class);
  private static final int MAX_PARTPICIPANTS = 1_000;

  public static void main (final String [] args)
  {
    try (final PDClient aClient = new PDClient ("http://localhost:8080"))
    {
      LOGGER.info ("Creating " + MAX_PARTPICIPANTS + " requests");
      for (int i = 0; i < MAX_PARTPICIPANTS; ++i)
      {
        // Real Belgian IDs start with 0 or 1
        final long nID = 1_000_000_000L + i;
        final IParticipantIdentifier aPI = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("0208:" +
                                                                                                                          StringHelper.getLeadingZero (nID,
                                                                                                                                                       10));
        aClient.addServiceGroupToIndex (aPI);
      }
    }
    catch (final InitializationException ex)
    {
      LOGGER.error ("Failed to invoke PDClient", ex);
    }
  }
}
