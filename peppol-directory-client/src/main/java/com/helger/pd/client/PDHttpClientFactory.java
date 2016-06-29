package com.helger.pd.client;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;

import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.httpclient.HttpClientFactory;
import com.helger.peppol.utils.LoadedKeyStore;

public final class PDHttpClientFactory extends HttpClientFactory
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (PDHttpClientFactory.class);

  @Override
  @Nullable
  public SSLContext createSSLContext () throws KeyManagementException,
                                        UnrecoverableKeyException,
                                        NoSuchAlgorithmException,
                                        KeyStoreException
  {
    // Load key store
    final LoadedKeyStore aLoadedKeyStore = LoadedKeyStore.loadKeyStore (PDClientConfiguration.getKeyStorePath (),
                                                                        PDClientConfiguration.getKeyStorePassword ());
    if (aLoadedKeyStore.isFailure ())
    {
      s_aLogger.error ("Failed to initialize keystore for service connection! Can only use http now! Details: " +
                       aLoadedKeyStore.getErrorMessage ());
      return null;
    }
    return SSLContexts.custom ()
                      .loadKeyMaterial (aLoadedKeyStore.getKeyStore (),
                                        PDClientConfiguration.getKeyStoreKeyPassword (),
                                        (aAliases, aSocket) -> {
                                          final String sAlias = PDClientConfiguration.getKeyStoreKeyAlias ();
                                          return aAliases.containsKey (sAlias) ? sAlias : null;
                                        })
                      .build ();
  }
}
