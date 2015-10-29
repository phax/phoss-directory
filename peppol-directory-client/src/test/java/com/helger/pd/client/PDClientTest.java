package com.helger.pd.client;

import org.junit.Test;

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
    aClient.deleteServiceGroupFromIndex (aPI);
    aClient.isServiceGroupRegistered (aPI);
    aClient.addServiceGroupToIndex (aPI);
  }
}
