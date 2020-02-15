/**
 * Copyright (C) 2015-2020 Philip Helger (www.helger.com)
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
import java.util.Arrays;

import javax.annotation.Nonnull;

import org.apache.http.HttpHost;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.ssl.PrivateKeyStrategy;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.EURLProtocol;
import com.helger.httpclient.HttpClientSettings;
import com.helger.peppol.utils.PeppolKeyStoreHelper;
import com.helger.security.keystore.LoadedKey;
import com.helger.security.keystore.LoadedKeyStore;

/**
 * Special {@link HttpClientSettings} that incorporates all the parameters from
 * "pd-client.properties" file.
 *
 * @author Philip Helger
 */
public class PDHttpClientSettings extends HttpClientSettings
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PDHttpClientSettings.class);

  public PDHttpClientSettings (@Nonnull @Nonempty final String sTargetURI)
  {
    resetToConfiguration (sTargetURI);
  }

  /**
   * Overwrite all settings that can appear in the configuration file.
   *
   * @param sTargetURI
   *        The target URI to connect to. Makes a difference if this is "http"
   *        or "https". MAy neither be <code>null</code> nor empty.
   */
  public final void resetToConfiguration (@Nonnull @Nonempty final String sTargetURI)
  {
    ValueEnforcer.notEmpty (sTargetURI, "TargetURI");
    final boolean bUseHttps = EURLProtocol.HTTPS.isUsedInURL (sTargetURI);

    // Proxy host
    final String sProxyHost = bUseHttps ? PDClientConfiguration.getHttpsProxyHost ()
                                        : PDClientConfiguration.getHttpProxyHost ();
    final int nProxyPort = bUseHttps ? PDClientConfiguration.getHttpsProxyPort ()
                                     : PDClientConfiguration.getHttpProxyPort ();
    if (sProxyHost != null && nProxyPort > 0)
    {
      LOGGER.info ("PD client uses proxy host");
      setProxyHost (new HttpHost (sProxyHost, nProxyPort));
    }
    else
      setProxyHost (null);

    // Proxy credentials
    final String sProxyUsername = PDClientConfiguration.getProxyUsername ();
    if (StringHelper.hasText (sProxyUsername))
    {
      LOGGER.info ("PD client uses proxy credentials");
      setProxyCredentials (new UsernamePasswordCredentials (sProxyUsername, PDClientConfiguration.getProxyPassword ()));
    }
    else
      setProxyCredentials (null);

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
                      PeppolKeyStoreHelper.getLoadError (aLoadedKeyStore));
      }
      else
      {
        LOGGER.info ("PD client keystore successfully loaded");

        // Sanity check if key can be loaded
        {
          final LoadedKey <PrivateKeyEntry> aLoadedKey = PDClientConfiguration.loadPrivateKey (aLoadedKeyStore.getKeyStore ());
          if (aLoadedKey.isFailure ())
            LOGGER.error ("PD client failed to initialize key from keystore. Details: " +
                          PeppolKeyStoreHelper.getLoadError (aLoadedKey));
          else
            LOGGER.info ("PD client key successfully loaded");
        }

        // Load trust store (may not be present/configured)
        final LoadedKeyStore aLoadedTrustStore = PDClientConfiguration.loadTrustStore ();
        if (aLoadedTrustStore.isFailure ())
          LOGGER.error ("PD client failed to initialize truststore for service connection. Details: " +
                        PeppolKeyStoreHelper.getLoadError (aLoadedTrustStore));
        else
          LOGGER.info ("PD client truststore successfully loaded");

        try
        {
          final PrivateKeyStrategy aPKS = (aAliases, aSocket) -> {
            if (LOGGER.isDebugEnabled ())
              LOGGER.debug ("chooseAlias(" + aAliases + ", " + aSocket + ")");

            final String sConfiguredAlias = PDClientConfiguration.getKeyStoreKeyAlias ();
            for (final String sCurAlias : aAliases.keySet ())
            {
              // Case insensitive alias handling
              if (sCurAlias.equalsIgnoreCase (sConfiguredAlias))
              {
                if (sCurAlias.equals (sConfiguredAlias))
                {
                  if (LOGGER.isDebugEnabled ())
                    LOGGER.debug ("  Chose alias '" + sCurAlias + "'");
                }
                else
                {
                  // Case insensitive match
                  if (LOGGER.isWarnEnabled ())
                    LOGGER.warn ("Chose the keystore alias '" +
                                 sCurAlias +
                                 "' but the configured alias '" +
                                 sConfiguredAlias +
                                 "' has a different casing. Please fix the configuration of the Directory client client-certificate.");
                }
                return sCurAlias;
              }
            }
            if (LOGGER.isWarnEnabled ())
              LOGGER.warn ("Found no client-certificate alias matching '" +
                           sConfiguredAlias +
                           "' in the provided aliases " +
                           aAliases.keySet ());
            return null;
          };
          final TrustStrategy aTS = (aChain, aAuthType) -> {
            if (LOGGER.isDebugEnabled ())
              LOGGER.debug ("isTrusted(" + Arrays.toString (aChain) + ", " + aAuthType + ")");
            return true;
          };
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
    setConnectionTimeoutMS (PDClientConfiguration.getConnectTimeoutMS ());
    setSocketTimeoutMS (PDClientConfiguration.getRequestTimeoutMS ());
  }
}
