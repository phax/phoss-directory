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
import java.security.KeyStore;
import java.util.Arrays;

import javax.annotation.Nonnull;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.ssl.PrivateKeyStrategy;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.httpclient.HttpClientFactory;
import com.helger.peppol.utils.PeppolKeyStoreHelper;
import com.helger.security.keystore.KeyStoreHelper;
import com.helger.security.keystore.LoadedKeyStore;

/**
 * Special {@link HttpClientFactory} that incorporates all the parameters from
 * "pd-client.properties" file.
 * 
 * @author Philip Helger
 */
public class PDHttpClientFactory extends HttpClientFactory
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PDHttpClientFactory.class);

  public PDHttpClientFactory ()
  {
    if (PDClientConfiguration.isHttpsHostnameVerificationDisabled ())
    {
      LOGGER.info ("PD client uses disabled hostname verification");
      setHostnameVerifierVerifyAll ();
    }

    // Load key store
    final LoadedKeyStore aLoadedKeyStore = KeyStoreHelper.loadKeyStore (PDClientConfiguration.getKeyStoreType (),
                                                                        PDClientConfiguration.getKeyStorePath (),
                                                                        PDClientConfiguration.getKeyStorePassword ());
    if (aLoadedKeyStore.isFailure ())
    {
      LOGGER.error ("PD client failed to initialize keystore for service connection! Can only use http now! Details: " +
                    PeppolKeyStoreHelper.getLoadError (aLoadedKeyStore));
    }
    else
    {
      LOGGER.info ("PD client keystore successfully loaded");
      // Load trust store (may not be present/configured)
      final LoadedKeyStore aLoadedTrustStore = KeyStoreHelper.loadKeyStore (PDClientConfiguration.getTrustStoreType (),
                                                                            PDClientConfiguration.getTrustStorePath (),
                                                                            PDClientConfiguration.getTrustStorePassword ());
      if (aLoadedTrustStore.isFailure ())
        LOGGER.error ("PD client failed to initialize truststore for service connection! Details: " +
                      PeppolKeyStoreHelper.getLoadError (aLoadedTrustStore));
      else
        LOGGER.info ("PD client truststore successfully loaded");

      final KeyStore aTrustStore = aLoadedTrustStore.getKeyStore ();

      try
      {
        final PrivateKeyStrategy aPKS = (aAliases, aSocket) -> {
          if (LOGGER.isDebugEnabled ())
            LOGGER.debug ("chooseAlias(" + aAliases + ", " + aSocket + ")");
          final String sAlias = PDClientConfiguration.getKeyStoreKeyAlias ();
          return aAliases.containsKey (sAlias) ? sAlias : null;
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
                                  .loadTrustMaterial (aTrustStore, aTS)
                                  .build ());
      }
      catch (final GeneralSecurityException ex)
      {
        throw new IllegalStateException ("PD client failed to set SSL context", ex);
      }
    }
  }

  @Override
  @Nonnull
  public RequestConfig.Builder createRequestConfigBuilder ()
  {
    final RequestConfig.Builder ret = super.createRequestConfigBuilder ();
    ret.setConnectTimeout (PDClientConfiguration.getConnectTimeoutMS ());
    ret.setSocketTimeout (PDClientConfiguration.getRequestTimeoutMS ());
    return ret;
  }
}
