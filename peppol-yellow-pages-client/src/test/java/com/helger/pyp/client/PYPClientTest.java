package com.helger.pyp.client;

import org.junit.Test;

import com.helger.peppol.identifier.participant.SimpleParticipantIdentifier;

/**
 * Test class for class {@link PYPClient}.
 *
 * @author Philip Helger
 */
public final class PYPClientTest
{
  static
  {
    PYPClientConfiguration.getConfigFile ().applyAllNetworkSystemProperties ();
  }

  @Test
  public void testBasic ()
  {
    final PYPClient aClient = PYPClient.createDefaultClient ();
    aClient.isParticipantRegistered (SimpleParticipantIdentifier.createWithDefaultScheme ("9915:test"));
  }
}
