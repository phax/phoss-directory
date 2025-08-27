/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.concurrent.ThreadHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.http.security.HostnameVerifierVerifyAll;
import com.helger.http.security.TrustManagerTrustAll;
import com.helger.pd.indexer.PDIndexerTestRule;
import com.helger.pd.indexer.businesscard.PDExtendedBusinessCard;
import com.helger.pd.indexer.clientcert.ClientCertificateValidator;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.peppol.businesscard.generic.PDBusinessCard;
import com.helger.peppol.businesscard.generic.PDBusinessEntity;
import com.helger.peppol.businesscard.generic.PDIdentifier;
import com.helger.peppol.businesscard.generic.PDName;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.peppolid.peppol.PeppolIdentifierHelper;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.security.keystore.EKeyStoreType;
import com.helger.security.keystore.KeyStoreHelper;
import com.helger.unittest.support.TestHelper;

import jakarta.annotation.Nonnull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;

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
      aEntity.names ().add (new PDName ("Philip's mock Peppol receiver"));
      aEntity.setCountryCode ("AT");
      aEntity.identifiers ().add (new PDIdentifier ("mock", "12345678"));
      aEntity.identifiers ().add (new PDIdentifier (aParticipantID.getScheme (), aParticipantID.getValue ()));
      aEntity.setAdditionalInfo ("This is a mock entry for testing purposes only");
      aBI.businessEntities ().add (aEntity);
    }
    {
      final PDBusinessEntity aEntity = new PDBusinessEntity ();
      aEntity.names ().add (new PDName ("Philip's mock Peppol receiver 2"));
      aEntity.setCountryCode ("NO");
      aEntity.identifiers ().add (new PDIdentifier ("mock", "abcdefgh"));
      aEntity.setAdditionalInfo ("This is another mock entry for testing purposes only");
      aBI.businessEntities ().add (aEntity);
    }
    return new PDExtendedBusinessCard (aBI,
                                       new CommonsArrayList <> (EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30.getAsDocumentTypeIdentifier ()));
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
                                                                    "peppol".toCharArray ());
      // Try to create the socket factory from the provided key store
      final KeyManagerFactory aKeyManagerFactory = KeyManagerFactory.getInstance ("SunX509");
      aKeyManagerFactory.init (aKeyStore, "peppol".toCharArray ());

      final SSLContext aSSLContext = SSLContext.getInstance ("TLS");
      aSSLContext.init (aKeyManagerFactory.getKeyManagers (),
                        new TrustManager [] { new TrustManagerTrustAll (false) },
                        null);
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
    if (m_aServer != null)
      m_aServer.shutdownNow ();
  }

  @Test
  public void testCreateAndDeleteParticipant () throws IOException
  {
    final AtomicInteger aIndex = new AtomicInteger (0);
    final PeppolIdentifierFactory aIF = PeppolIdentifierFactory.INSTANCE;

    // Create
    final int nCount = 4;
    TestHelper.testInParallel (nCount, () -> {
      final IParticipantIdentifier aPI = aIF.createParticipantIdentifierWithDefaultScheme ("9915:test" +
                                                                                           aIndex.getAndIncrement ());

      final String sPayload = aPI.getURIPercentEncoded ();
      LOGGER.info ("PUT " + sPayload);
      final String sResponseMsg = m_aTarget.path ("1.0").request ().put (Entity.text (sPayload), String.class);
      assertEquals ("", sResponseMsg);
    });

    LOGGER.info ("waiting");
    ThreadHelper.sleep (2000);
    for (int i = 0; i < nCount; ++i)
    {
      final IParticipantIdentifier aPI = aIF.createParticipantIdentifierWithDefaultScheme ("9915:test" + i);
      assertTrue (PDMetaManager.getStorageMgr ().containsEntry (aPI));
    }

    // Delete
    aIndex.set (0);
    TestHelper.testInParallel (nCount, () -> {
      final IParticipantIdentifier aPI = aIF.createParticipantIdentifierWithDefaultScheme ("9915:test" +
                                                                                           aIndex.getAndIncrement ());

      final String sPI = aPI.getURIEncoded ();
      LOGGER.info ("DELETE " + sPI);
      final String sResponseMsg = m_aTarget.path ("1.0").path (sPI).request ().delete (String.class);
      assertEquals ("", sResponseMsg);
    });

    LOGGER.info ("waiting");
    ThreadHelper.sleep (2000);
    for (int i = 0; i < nCount; ++i)
    {
      final IParticipantIdentifier aPI = aIF.createParticipantIdentifierWithDefaultScheme ("9915:test" + i);
      assertFalse (PDMetaManager.getStorageMgr ().containsEntry (aPI));
    }

    // Test with invalid URL encoded ID
    {
      final String sPayload = "iso6523-actorid-upis%3a%3a9915%3atest0%%%abc";
      LOGGER.info ("CREATE " + sPayload);
      try
      {
        m_aTarget.path ("1.0").request ().put (Entity.text (sPayload), String.class);
        fail ();
      }
      catch (final BadRequestException ex)
      {
        // Expected
      }
    }
  }
}
