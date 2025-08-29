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
package com.helger.pd.client;

import java.security.GeneralSecurityException;
import java.security.KeyStore.PrivateKeyEntry;

import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.ssl.PrivateKeyStrategy;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.httpclient.HttpClientSettings;
import com.helger.httpclient.security.PrivateKeyStrategyFromAliasCaseInsensitive;
import com.helger.httpclient.security.TrustStrategyTrustAll;
import com.helger.security.keystore.LoadedKey;
import com.helger.security.keystore.LoadedKeyStore;
import com.helger.url.protocol.EURLProtocol;

import jakarta.annotation.Nonnull;

/**
 * Special {@link HttpClientSettings} that incorporates all the parameters from
 * "pd-client.properties" file.
 *
 * @author Philip Helger
 */
public class PDHttpClientSettings extends HttpClientSettings
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PDHttpClientSettings.class);

  /**
   * Constructor.
   *
   * @param sTargetURI
   *        The target URI of the Directory use. May neither be <code>null</code> nor empty.
   * @throws IllegalStateException
   *         If the "https" protocol is used, and the SSL setup is incomplete.
   */
  public PDHttpClientSettings (@Nonnull @Nonempty final String sTargetURI)
  {
    resetToConfiguration (sTargetURI);
  }

  /**
   * Overwrite all settings that can appear in the configuration file "pd-client.properties".
   *
   * @param sTargetURI
   *        The target URI to connect to. Makes a difference if this is "http" or "https". May
   *        neither be <code>null</code> nor empty.
   */
  public final void resetToConfiguration (@Nonnull @Nonempty final String sTargetURI)
  {
    ValueEnforcer.notEmpty (sTargetURI, "TargetURI");
    final boolean bUseHttps = EURLProtocol.HTTPS.isUsedInURL (sTargetURI);

    // Proxy host
    final String sProxyHost = PDClientConfiguration.getHttpProxyHost ();
    final int nProxyPort = PDClientConfiguration.getHttpProxyPort ();
    if (sProxyHost != null && nProxyPort > 0)
    {
      final HttpHost aProxyHost = new HttpHost (sProxyHost, nProxyPort);
      LOGGER.info ("PD client uses proxy host " + aProxyHost);
      getGeneralProxy ().setProxyHost (aProxyHost);
    }
    else
      getGeneralProxy ().setProxyHost (null);

    // Proxy credentials
    final String sProxyUsername = PDClientConfiguration.getProxyUsername ();
    if (StringHelper.isNotEmpty (sProxyUsername))
    {
      LOGGER.info ("PD client uses proxy credentials");
      getGeneralProxy ().setProxyCredentials (new UsernamePasswordCredentials (sProxyUsername,
                                                                               PDClientConfiguration.getProxyPassword ()));
    }
    else
      getGeneralProxy ().setProxyCredentials (null);

    // Reset SSL stuff
    setHostnameVerifier (null);
    setSSLContext (null);

    if (bUseHttps)
    {
      if (PDClientConfiguration.isHttpsHostnameVerificationDisabled ())
      {
        LOGGER.info ("PD client uses disabled hostname verification");
        setHostnameVerifierVerifyAll ();
      }

      // Load key store
      final LoadedKeyStore aLoadedKeyStore = PDClientConfiguration.loadKeyStore ();
      if (aLoadedKeyStore.isFailure ())
      {
        LOGGER.error ("PD client failed to initialize keystore for service connection - can only use http now! Details: " +
                      LoadedKeyStore.getLoadError (aLoadedKeyStore));
      }
      else
      {
        LOGGER.info ("PD client keystore successfully loaded");

        // Sanity check if key can be loaded
        {
          final LoadedKey <PrivateKeyEntry> aLoadedKey = PDClientConfiguration.loadPrivateKey (aLoadedKeyStore.getKeyStore ());
          if (aLoadedKey.isFailure ())
          {
            LOGGER.error ("PD client failed to initialize key from keystore. Details: " +
                          LoadedKey.getLoadError (aLoadedKey));
          }
          else
            LOGGER.info ("PD client key successfully loaded");
        }

        // Load trust store (may not be present/configured)
        final LoadedKeyStore aLoadedTrustStore = PDClientConfiguration.loadTrustStore ();
        if (aLoadedTrustStore.isFailure ())
          LOGGER.error ("PD client failed to initialize truststore for service connection. Details: " +
                        LoadedKeyStore.getLoadError (aLoadedTrustStore));
        else
          LOGGER.info ("PD client truststore successfully loaded");

        try
        {
          final PrivateKeyStrategy aPKS = new PrivateKeyStrategyFromAliasCaseInsensitive (PDClientConfiguration.getKeyStoreKeyAlias ());
          final TrustStrategy aTS = new TrustStrategyTrustAll ();
          setSSLContext (SSLContexts.custom ()
                                    .loadKeyMaterial (aLoadedKeyStore.getKeyStore (),
                                                      PDClientConfiguration.getKeyStoreKeyPassword (),
                                                      aPKS)
                                    .loadTrustMaterial (aLoadedTrustStore.getKeyStore (), aTS)
                                    .build ());
          LOGGER.info ("PD client successfully set SSL context");
        }
        catch (final GeneralSecurityException ex)
        {
          throw new IllegalStateException ("PD client failed to set SSL context", ex);
        }
      }
    }

    // Timeouts
    setConnectTimeout (PDClientConfiguration.getConnectTimeout ());
    setResponseTimeout (PDClientConfiguration.getResponseTimeout ());
  }
}
