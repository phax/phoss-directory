package com.helger.pd.client;

import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;

import org.apache.http.HttpHost;
import org.apache.http.ssl.SSLContexts;

import com.helger.httpclient.HttpClientWrapper;
import com.helger.peppol.utils.KeyStoreHelper;

public final class PDHttpClientWrapper extends HttpClientWrapper
{
  private final HttpHost m_aProxy;

  public PDHttpClientWrapper (@Nullable final HttpHost aProxy)
  {
    m_aProxy = aProxy;
  }

  @Override
  public SSLContext createSSLContext () throws GeneralSecurityException
  {
    try
    {
      // Set SSL context
      final KeyStore aKeyStore = KeyStoreHelper.loadKeyStore (PDClientConfiguration.getKeyStorePath (),
                                                              PDClientConfiguration.getKeyStorePassword ());
      return SSLContexts.custom ()
                        .loadKeyMaterial (aKeyStore,
                                          PDClientConfiguration.getKeyStoreKeyPassword (),
                                          (aAliases, aSocket) -> {
                                            final String sAlias = PDClientConfiguration.getKeyStoreKeyAlias ();
                                            return aAliases.containsKey (sAlias) ? sAlias : null;
                                          })
                        .build ();
    }
    catch (final Throwable t)
    {
      PDClient.s_aLogger.error ("Failed to initialize keystore for service connection! Can only use http now!", t);
    }
    return null;
  }

  @Override
  public HttpHost createProxyHost ()
  {
    return m_aProxy;
  }
}
