/**
 * Copyright (C) 2015 Philip Helger (www.helger.com)
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
package com.helger.pyp.indexer.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;

import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.mock.CommonsTestHelper;
import com.helger.commons.random.VerySecureRandom;
import com.helger.commons.thread.ThreadHelper;
import com.helger.peppol.identifier.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppol.identifier.participant.IPeppolParticipantIdentifier;
import com.helger.peppol.identifier.participant.SimpleParticipantIdentifier;
import com.helger.peppol.utils.KeyStoreHelper;
import com.helger.pyp.businessinformation.BusinessInformationType;
import com.helger.pyp.businessinformation.EntityType;
import com.helger.pyp.businessinformation.IdentifierType;
import com.helger.pyp.businessinformation.PYPExtendedBusinessInformation;
import com.helger.pyp.indexer.PYPIndexerTestRule;
import com.helger.pyp.indexer.clientcert.ClientCertificateValidator;
import com.helger.pyp.indexer.mgr.PYPIndexerManager;
import com.helger.pyp.indexer.mgr.PYPMetaManager;
import com.helger.web.https.DoNothingTrustManager;
import com.helger.web.https.HostnameVerifierAlwaysTrue;

/**
 * Test class for class {@link IndexerResource}.
 *
 * @author Philip Helger
 */
public final class IndexerResourceTest
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (IndexerResourceTest.class);

  @Rule
  public final TestRule m_aRule = new PYPIndexerTestRule ();

  private HttpServer m_aServer;
  private WebTarget m_aTarget;

  @Nonnull
  private static PYPExtendedBusinessInformation _createMockBI (@Nonnull final IPeppolParticipantIdentifier aParticipantID)
  {
    final BusinessInformationType aBI = new BusinessInformationType ();
    {
      final EntityType aEntity = new EntityType ();
      aEntity.setCountryCode ("AT");
      aEntity.setName ("Philip's mock PEPPOL receiver");
      IdentifierType aID = new IdentifierType ();
      aID.setType ("mock");
      aID.setValue ("12345678");
      aEntity.addIdentifier (aID);
      aID = new IdentifierType ();
      aID.setType ("provided");
      aID.setValue (aParticipantID.getURIEncoded ());
      aEntity.addIdentifier (aID);
      aEntity.setFreeText ("This is a mock entry for testing purposes only");
      aBI.addEntity (aEntity);
    }
    {
      final EntityType aEntity = new EntityType ();
      aEntity.setCountryCode ("NO");
      aEntity.setName ("Philip's mock PEPPOL receiver 2");
      final IdentifierType aID = new IdentifierType ();
      aID.setType ("mock");
      aID.setValue ("abcdefgh");
      aEntity.addIdentifier (aID);
      aEntity.setFreeText ("This is another mock entry for testing purposes only");
      aBI.addEntity (aEntity);
    }
    return new PYPExtendedBusinessInformation (aBI,
                                               CollectionHelper.newList (EPredefinedDocumentTypeIdentifier.INVOICE_T010_BIS5A_V20.getAsDocumentTypeIdentifier ()));
  }

  @Before
  public void setUp () throws GeneralSecurityException, IOException
  {
    // Set test BI provider
    PYPMetaManager.setIndexerMgrFactory (aStorageMgr -> new PYPIndexerManager (aStorageMgr).setBusinessInformationProvider (aParticipantID -> _createMockBI (aParticipantID))
                                                                                           .readAndQueueInitialData ());
    PYPMetaManager.getInstance ();

    final File aTestClientCertificateKeyStore = new File ("src/test/resources/smp.pilot.jks");
    if (aTestClientCertificateKeyStore.exists ())
    {
      // https
      m_aServer = MockServer.startSecureServer ();

      final KeyStore aKeyStore = KeyStoreHelper.loadKeyStore (aTestClientCertificateKeyStore.getAbsolutePath (),
                                                              "peppol");
      // Try to create the socket factory from the provided key store
      final KeyManagerFactory aKeyManagerFactory = KeyManagerFactory.getInstance ("SunX509");
      aKeyManagerFactory.init (aKeyStore, "peppol".toCharArray ());

      final SSLContext aSSLContext = SSLContext.getInstance ("TLS");
      aSSLContext.init (aKeyManagerFactory.getKeyManagers (),
                        new TrustManager [] { new DoNothingTrustManager (false) },
                        VerySecureRandom.getInstance ());
      final Client aClient = ClientBuilder.newBuilder ()
                                          .sslContext (aSSLContext)
                                          .hostnameVerifier (new HostnameVerifierAlwaysTrue (false))
                                          .build ();
      m_aTarget = aClient.target (MockServer.BASE_URI_HTTPS);
    }
    else
    {
      // http only
      s_aLogger.warn ("The SMP pilot keystore is missing for the tests! Client certificate handling will not be tested!");
      ClientCertificateValidator.allowAllForTests (true);

      m_aServer = MockServer.startRegularServer ();

      final Client aClient = ClientBuilder.newClient ();
      m_aTarget = aClient.target (MockServer.BASE_URI_HTTP);
    }
  }

  @After
  public void tearDown ()
  {
    m_aServer.shutdownNow ();
  }

  @Test
  public void testCreateAndDeleteParticipant () throws IOException
  {
    final AtomicInteger aIndex = new AtomicInteger (0);
    final SimpleParticipantIdentifier aPI_0 = SimpleParticipantIdentifier.createWithDefaultScheme ("9915:test0");

    final int nCount = 4;
    CommonsTestHelper.testInParallel (nCount, (Runnable) () -> {
      // Create
      final SimpleParticipantIdentifier aPI = SimpleParticipantIdentifier.createWithDefaultScheme ("9915:test" +
                                                                                                   aIndex.getAndIncrement ());

      final String sResponseMsg = m_aTarget.path ("1.0").request ().put (Entity.text (aPI.getURIEncoded ()),
                                                                         String.class);
      assertEquals ("", sResponseMsg);
    });

    ThreadHelper.sleep (2000);
    assertTrue (PYPMetaManager.getStorageMgr ().containsEntry (aPI_0));

    aIndex.set (0);
    CommonsTestHelper.testInParallel (nCount, (Runnable) () -> {
      // Delete
      final SimpleParticipantIdentifier aPI = SimpleParticipantIdentifier.createWithDefaultScheme ("9915:test" +
                                                                                                   aIndex.getAndIncrement ());

      final String sResponseMsg = m_aTarget.path ("1.0").path (aPI.getURIEncoded ()).request ().delete (String.class);
      assertEquals ("", sResponseMsg);
    });

    ThreadHelper.sleep (2000);
    assertFalse (PYPMetaManager.getStorageMgr ().containsEntry (aPI_0));
  }
}
