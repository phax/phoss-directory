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
package com.helger.pd.indexer.rest;

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

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.concurrent.ThreadHelper;
import com.helger.commons.mock.CommonsTestHelper;
import com.helger.commons.random.RandomHelper;
import com.helger.commons.ws.HostnameVerifierVerifyAll;
import com.helger.commons.ws.TrustManagerTrustAll;
import com.helger.pd.businesscard.generic.PDBusinessCard;
import com.helger.pd.businesscard.generic.PDBusinessEntity;
import com.helger.pd.businesscard.generic.PDIdentifier;
import com.helger.pd.businesscard.generic.PDName;
import com.helger.pd.indexer.PDIndexerTestRule;
import com.helger.pd.indexer.businesscard.PDExtendedBusinessCard;
import com.helger.pd.indexer.clientcert.ClientCertificateValidator;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.indexer.storage.EQueryMode;
import com.helger.peppol.identifier.factory.PeppolIdentifierFactory;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.identifier.peppol.PeppolIdentifierHelper;
import com.helger.peppol.identifier.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.security.keystore.EKeyStoreType;
import com.helger.security.keystore.KeyStoreHelper;

/**
 * Test class for class {@link IndexerResource}.
 *
 * @author Philip Helger
 */
public final class IndexerResourceTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (IndexerResourceTest.class);

  @Rule
  public final TestRule m_aRule = new PDIndexerTestRule ();

  private HttpServer m_aServer;
  private WebTarget m_aTarget;

  @Nonnull
  private static PDExtendedBusinessCard _createMockBC (@Nonnull final IParticipantIdentifier aParticipantID)
  {
    final PDBusinessCard aBI = new PDBusinessCard ();
    aBI.setParticipantIdentifier (new PDIdentifier (PeppolIdentifierHelper.DEFAULT_PARTICIPANT_SCHEME, "9915:mock"));
    {
      final PDBusinessEntity aEntity = new PDBusinessEntity ();
      aEntity.names ().add (new PDName ("Philip's mock PEPPOL receiver"));
      aEntity.setCountryCode ("AT");
      aEntity.identifiers ().add (new PDIdentifier ("mock", "12345678"));
      aEntity.identifiers ().add (new PDIdentifier (aParticipantID.getScheme (), aParticipantID.getValue ()));
      aEntity.setAdditionalInfo ("This is a mock entry for testing purposes only");
      aBI.businessEntities ().add (aEntity);
    }
    {
      final PDBusinessEntity aEntity = new PDBusinessEntity ();
      aEntity.names ().add (new PDName ("Philip's mock PEPPOL receiver 2"));
      aEntity.setCountryCode ("NO");
      aEntity.identifiers ().add (new PDIdentifier ("mock", "abcdefgh"));
      aEntity.setAdditionalInfo ("This is another mock entry for testing purposes only");
      aBI.businessEntities ().add (aEntity);
    }
    return new PDExtendedBusinessCard (aBI,
                                       new CommonsArrayList <> (EPredefinedDocumentTypeIdentifier.INVOICE_T010_BIS5A_V20.getAsDocumentTypeIdentifier ()));
  }

  @Before
  public void setUp () throws GeneralSecurityException, IOException
  {
    // Set test BC provider first!
    PDMetaManager.setBusinessCardProvider (IndexerResourceTest::_createMockBC);
    PDMetaManager.getInstance ();

    final File aTestClientCertificateKeyStore = new File ("src/test/resources/smp.pilot.jks");
    if (aTestClientCertificateKeyStore.exists ())
    {
      // https
      m_aServer = MockServer.startSecureServer ();

      final KeyStore aKeyStore = KeyStoreHelper.loadKeyStoreDirect (EKeyStoreType.JKS,
                                                                    aTestClientCertificateKeyStore.getAbsolutePath (),
                                                                    "peppol");
      // Try to create the socket factory from the provided key store
      final KeyManagerFactory aKeyManagerFactory = KeyManagerFactory.getInstance ("SunX509");
      aKeyManagerFactory.init (aKeyStore, "peppol".toCharArray ());

      final SSLContext aSSLContext = SSLContext.getInstance ("TLS");
      aSSLContext.init (aKeyManagerFactory.getKeyManagers (),
                        new TrustManager [] { new TrustManagerTrustAll (false) },
                        RandomHelper.getSecureRandom ());
      final Client aClient = ClientBuilder.newBuilder ()
                                          .sslContext (aSSLContext)
                                          .hostnameVerifier (new HostnameVerifierVerifyAll (false))
                                          .build ();
      m_aTarget = aClient.target (MockServer.BASE_URI_HTTPS);
    }
    else
    {
      // http only
      LOGGER.warn ("The SMP pilot keystore is missing for the tests! Client certificate handling will not be tested!");
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
    final IParticipantIdentifier aPI_0 = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:test0");

    final int nCount = 4;
    CommonsTestHelper.testInParallel (nCount, () -> {
      // Create
      final IParticipantIdentifier aPI = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:test" +
                                                                                                                        aIndex.getAndIncrement ());

      LOGGER.info ("PUT " + aPI.getURIEncoded ());
      final String sPayload = aPI.getURIEncoded ();
      final String sResponseMsg = m_aTarget.path ("1.0").request ().put (Entity.text (sPayload), String.class);
      assertEquals ("", sResponseMsg);
    });

    ThreadHelper.sleep (2000);
    assertTrue (PDMetaManager.getStorageMgr ().containsEntry (aPI_0, EQueryMode.NON_DELETED_ONLY));
    assertFalse (PDMetaManager.getStorageMgr ().containsEntry (aPI_0, EQueryMode.DELETED_ONLY));

    aIndex.set (0);
    CommonsTestHelper.testInParallel (nCount, () -> {
      // Delete
      final IParticipantIdentifier aPI = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:test" +
                                                                                                                        aIndex.getAndIncrement ());

      LOGGER.info ("DELETE " + aPI.getURIEncoded ());
      final String sResponseMsg = m_aTarget.path ("1.0").path (aPI.getURIEncoded ()).request ().delete (String.class);
      assertEquals ("", sResponseMsg);
    });

    ThreadHelper.sleep (2000);
    assertFalse (PDMetaManager.getStorageMgr ().containsEntry (aPI_0, EQueryMode.NON_DELETED_ONLY));
  }
}
