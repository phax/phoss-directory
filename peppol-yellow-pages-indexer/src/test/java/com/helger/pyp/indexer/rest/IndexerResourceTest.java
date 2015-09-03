package com.helger.pyp.indexer.rest;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for class {@link IndexerResource}.
 *
 * @author Philip Helger
 */
public final class IndexerResourceTest
{
  private HttpServer m_aServer;
  private WebTarget m_aTarget;

  @Before
  public void setUp () throws Exception
  {
    m_aServer = Main.startServer ();
    final Client aClient = ClientBuilder.newClient ();
    m_aTarget = aClient.target (Main.BASE_URI);
  }

  @After
  public void tearDown () throws Exception
  {
    m_aServer.shutdownNow ();
  }

  @Test
  public void testCreateOrUpdateParticipant ()
  {
    final String sResponseMsg = m_aTarget.path ("1.0").path ("9915:test").request ().get (String.class);
    assertEquals ("Got it: '9915:test'", sResponseMsg);
  }
}
