package com.helger.pyp.indexer.rest;

import static org.junit.Assert.assertEquals;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.helger.commons.random.VerySecureRandom;
import com.helger.web.https.DoNothingTrustManager;
import com.helger.web.https.HostnameVerifierAlwaysTrue;

/**
 * Test class for class {@link IndexerResource}.
 *
 * @author Philip Helger
 */
public final class IndexerResourceTest
{
  static
  {
    SLF4JBridgeHandler.removeHandlersForRootLogger ();
    SLF4JBridgeHandler.install ();
  }

  private HttpServer m_aServer;
  private WebTarget m_aTarget;

  @Before
  public void setUp () throws NoSuchAlgorithmException, KeyManagementException
  {
    if (true)
    {
      // https
      m_aServer = Main.startSecureServer ();

      final SSLContext aSSLContext = SSLContext.getInstance ("TLS");
      aSSLContext.init (null,
                        new TrustManager [] { new DoNothingTrustManager (false) },
                        VerySecureRandom.getInstance ());
      final Client aClient = ClientBuilder.newBuilder ()
                                          .sslContext (aSSLContext)
                                          .hostnameVerifier (new HostnameVerifierAlwaysTrue (false))
                                          .build ();
      m_aTarget = aClient.target (Main.BASE_URI);
    }
    else
    {
      // http
      m_aServer = Main.startRegularServer ();

      final Client aClient = ClientBuilder.newClient ();
      m_aTarget = aClient.target (Main.BASE_URI);
    }
  }

  @After
  public void tearDown ()
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
