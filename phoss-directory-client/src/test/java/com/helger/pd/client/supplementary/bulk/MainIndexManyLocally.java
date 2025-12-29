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
