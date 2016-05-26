/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.CodingErrorAction;
import java.security.KeyStore;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.PrivateKeyDetails;
import org.apache.http.ssl.PrivateKeyStrategy;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.OverrideOnDemand;
import com.helger.commons.charset.CCharset;
import com.helger.commons.exception.InitializationException;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.state.ESuccess;
import com.helger.peppol.identifier.IParticipantIdentifier;
import com.helger.peppol.identifier.IdentifierHelper;
import com.helger.peppol.utils.KeyStoreHelper;

/**
 * This class is used for calling the PD indexer REST interface.
 *
 * @author Philip Helger
 */
public class PDClient implements Closeable
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (PDClient.class);

  /**
   * The string representation of the PEPPOL Directory host URL, always ending
   * with a trailing slash!
   */
  private final String m_sPDHost;
  private final String m_sPDIndexerURL;

  private HttpHost m_aProxy;
  private Credentials m_aProxyCredentials;
  private CloseableHttpClient m_aHttpClient;

  /**
   * Constructor with a direct PEPPOL Directory URL.
   *
   * @param aPDHost
   *        The address of the PEPPOL Directory Server including the application
   *        server context path but without the REST interface. May be http or
   *        https. Example: http://pyp.helger.com/
   */
  public PDClient (@Nonnull final URI aPDHost)
  {
    ValueEnforcer.notNull (aPDHost, "PDHost");

    // Build string and ensure it ends with a "/"
    final String sSMPHost = aPDHost.toString ();
    m_sPDHost = sSMPHost.endsWith ("/") ? sSMPHost : sSMPHost + '/';
    m_sPDIndexerURL = m_sPDHost + "indexer/1.0/";
  }

  /**
   * @return The PEPPOL Directory host URI string we're operating on. Never
   *         <code>null</code> . Always has a trailing "/".
   */
  @Nonnull
  public String getPDHostURI ()
  {
    return m_sPDHost;
  }

  /**
   * @return The HTTP proxy to be used to access the PEPPOL Directory server. Is
   *         <code>null</code> by default.
   */
  @Nullable
  public HttpHost getProxy ()
  {
    return m_aProxy;
  }

  /**
   * Set the proxy to be used to access the PEPPOL Directory server.
   *
   * @param aProxy
   *        May be <code>null</code> to indicate no proxy.
   */
  public void setProxy (@Nullable final HttpHost aProxy)
  {
    m_aProxy = aProxy;
  }

  /**
   * @return The HTTP proxy credentials to be used to access the PEPPOL
   *         Directory server. Is <code>null</code> by default.
   */
  @Nullable
  public Credentials getProxyCredentials ()
  {
    return m_aProxyCredentials;
  }

  /**
   * Set the proxy Credentials to be used to access the PEPPOL Directory server.
   *
   * @param aProxyCredentials
   *        May be <code>null</code> to indicate no proxy credentials necessary.
   */
  public void setProxyCredentials (@Nullable final Credentials aProxyCredentials)
  {
    m_aProxyCredentials = aProxyCredentials;
  }

  protected HttpClientBuilder createClientBuilder ()
  {
    try
    {
      // Set SSL context
      final KeyStore aKeyStore = KeyStoreHelper.loadKeyStore (PDClientConfiguration.getKeyStorePath (),
                                                              PDClientConfiguration.getKeyStorePassword ());
      final SSLContext aSSLContext = SSLContexts.custom ()
                                                .loadKeyMaterial (aKeyStore,
                                                                  PDClientConfiguration.getKeyStoreKeyPassword (),
                                                                  new PrivateKeyStrategy ()
                                                                  {
                                                                    public String chooseAlias (final Map <String, PrivateKeyDetails> aAliases,
                                                                                               final Socket aSocket)
                                                                    {
                                                                      final String sAlias = PDClientConfiguration.getKeyStoreKeyAlias ();
                                                                      return aAliases.containsKey (sAlias) ? sAlias
                                                                                                           : null;
                                                                    }
                                                                  })
                                                .build ();
      // Allow TLSv1 protocol only
      final SSLConnectionSocketFactory aSSLSocketFactory = new SSLConnectionSocketFactory (aSSLContext,
                                                                                           new String [] { "TLSv1" },
                                                                                           null,
                                                                                           SSLConnectionSocketFactory.getDefaultHostnameVerifier ());

      final Registry <ConnectionSocketFactory> sfr = RegistryBuilder.<ConnectionSocketFactory> create ()
                                                                    .register ("http",
                                                                               PlainConnectionSocketFactory.getSocketFactory ())
                                                                    .register ("https", aSSLSocketFactory)
                                                                    .build ();

      final PoolingHttpClientConnectionManager aConnMgr = new PoolingHttpClientConnectionManager (sfr);
      aConnMgr.setDefaultMaxPerRoute (100);
      aConnMgr.setMaxTotal (200);
      aConnMgr.setValidateAfterInactivity (1000);
      final ConnectionConfig aConnectionConfig = ConnectionConfig.custom ()
                                                                 .setMalformedInputAction (CodingErrorAction.IGNORE)
                                                                 .setUnmappableInputAction (CodingErrorAction.IGNORE)
                                                                 .setCharset (Consts.UTF_8)
                                                                 .build ();
      aConnMgr.setDefaultConnectionConfig (aConnectionConfig);

      return HttpClientBuilder.create ().setConnectionManager (aConnMgr);
    }
    catch (final Exception ex)
    {
      throw new InitializationException ("Failed to init HTTP client", ex);
    }
  }

  @Nonnull
  @OverrideOnDemand
  protected RequestConfig createRequestConfig ()
  {
    return RequestConfig.custom ()
                        .setSocketTimeout (10000)
                        .setConnectTimeout (5000)
                        .setConnectionRequestTimeout (5000)
                        .setProxy (m_aProxy)
                        .build ();
  }

  /**
   * The main execution routine. Overwrite this method to add additional
   * properties to the call.
   *
   * @param aRequest
   *        The request to be executed. Never <code>null</code>.
   * @return The HTTP execution response. Never <code>null</code>.
   * @throws IOException
   *         On HTTP error
   */
  @Nonnull
  @OverrideOnDemand
  protected CloseableHttpResponse executeRequest (@Nonnull final HttpRequestBase aRequest) throws IOException
  {
    aRequest.setConfig (createRequestConfig ());

    // Contextual attributes set the local context level will take
    // precedence over those set at the client level.
    final HttpClientContext aContext = HttpClientContext.create ();
    if (m_aProxy != null && m_aProxyCredentials != null)
    {
      final CredentialsProvider aCredentialsProvider = new BasicCredentialsProvider ();
      aCredentialsProvider.setCredentials (new AuthScope (m_aProxy), m_aProxyCredentials);
      aContext.setCredentialsProvider (aCredentialsProvider);
    }

    if (m_aHttpClient == null)
      m_aHttpClient = createClientBuilder ().build ();

    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug ("Executing request " + aRequest.getRequestLine ());

    return m_aHttpClient.execute (aRequest, aContext);
  }

  @Nullable
  private static String _getResponseString (@Nonnull final CloseableHttpResponse aResponse) throws IOException
  {
    final HttpEntity aResponseEntity = aResponse.getEntity ();
    final String sResponse = aResponseEntity == null ? null
                                                     : StreamHelper.getAllBytesAsString (aResponseEntity.getContent (),
                                                                                         CCharset.CHARSET_UTF_8_OBJ);
    EntityUtils.consume (aResponseEntity);
    return sResponse;
  }

  /**
   * Gets a list of references to the CompleteServiceGroup's owned by the
   * specified userId. This is a non-specification compliant method.
   *
   * @param aParticipantID
   *        Participant ID to query for existence. May not be <code>null</code>.
   * @return <code>true</code> if the participant is in the index,
   *         <code>false</code> otherwise.
   */
  @Nonnull
  public boolean isServiceGroupRegistered (@Nonnull final IParticipantIdentifier aParticipantID)
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");

    final HttpGet aGet = new HttpGet (m_sPDIndexerURL +
                                      IdentifierHelper.getIdentifierURIPercentEncoded (aParticipantID));
    CloseableHttpResponse aResponse = null;
    try
    {
      aResponse = executeRequest (aGet);
      final String sResponse = _getResponseString (aResponse);

      // Check result
      if (aResponse.getStatusLine ().getStatusCode () >= 200 && aResponse.getStatusLine ().getStatusCode () < 300)
        return true;

      if (aResponse.getStatusLine ().getStatusCode () == 404)
        return false;

      s_aLogger.warn ("Unexpected status returned from server for " +
                      aGet.getRequestLine () +
                      ": " +
                      aResponse.getStatusLine () +
                      "\n" +
                      sResponse);
    }
    catch (final IOException ex)
    {
      s_aLogger.error ("Error performing request " + aGet.getRequestLine (), ex);
    }
    finally
    {
      StreamHelper.close (aResponse);
    }
    return false;
  }

  @Nonnull
  public ESuccess addServiceGroupToIndex (@Nonnull final IParticipantIdentifier aParticipantID)
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    final String sParticipantID = IdentifierHelper.getIdentifierURIEncoded (aParticipantID);

    final HttpPut aPut = new HttpPut (m_sPDIndexerURL);
    aPut.setEntity (new StringEntity (sParticipantID, CCharset.CHARSET_UTF_8_OBJ));
    CloseableHttpResponse aResponse = null;
    try
    {
      aResponse = executeRequest (aPut);
      final String sResponse = _getResponseString (aResponse);

      // Check result
      if (aResponse.getStatusLine ().getStatusCode () >= 200 && aResponse.getStatusLine ().getStatusCode () < 300)
      {
        s_aLogger.info ("Added service group '" +
                        sParticipantID +
                        "' to PEPPOL Directory index. May take some time until it shows up.");
        return ESuccess.SUCCESS;
      }

      s_aLogger.warn ("Unexpected status returned from server for " +
                      aPut.getRequestLine () +
                      ": " +
                      aResponse.getStatusLine () +
                      "\n" +
                      sResponse);
    }
    catch (final IOException ex)
    {
      s_aLogger.error ("Error performing request " + aPut.getRequestLine (), ex);
    }
    finally
    {
      StreamHelper.close (aResponse);
    }
    return ESuccess.FAILURE;
  }

  @Nonnull
  public ESuccess deleteServiceGroupFromIndex (@Nonnull final IParticipantIdentifier aParticipantID)
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");

    final HttpDelete aDelete = new HttpDelete (m_sPDIndexerURL +
                                               IdentifierHelper.getIdentifierURIPercentEncoded (aParticipantID));
    CloseableHttpResponse aResponse = null;
    try
    {
      aResponse = executeRequest (aDelete);
      final String sResponse = _getResponseString (aResponse);

      // Check result
      if (aResponse.getStatusLine ().getStatusCode () >= 200 && aResponse.getStatusLine ().getStatusCode () < 300)
      {
        final String sParticipantID = IdentifierHelper.getIdentifierURIEncoded (aParticipantID);
        s_aLogger.info ("Removed service group '" +
                        sParticipantID +
                        "' from PEPPOL Directory index. May take some time until it is removed.");
        return ESuccess.SUCCESS;
      }

      s_aLogger.warn ("Unexpected status returned from server for " +
                      aDelete.getRequestLine () +
                      ": " +
                      aResponse.getStatusLine () +
                      "\n" +
                      sResponse);
    }
    catch (final IOException ex)
    {
      s_aLogger.error ("Error performing request " + aDelete.getRequestLine (), ex);
    }
    finally
    {
      StreamHelper.close (aResponse);
    }
    return ESuccess.FAILURE;
  }

  public void close () throws IOException
  {
    if (m_aHttpClient != null)
      m_aHttpClient.close ();
  }

  @Nonnull
  public static PDClient createDefaultClient ()
  {
    final PDClient aClient = new PDClient (URI.create ("http://pyp.helger.com"));
    final boolean bIsHttp = aClient.getPDHostURI ().startsWith ("http:");

    // Get proxy settings
    final String sPrefix = bIsHttp ? "http." : "https.";
    final String sProxyHost = PDClientConfiguration.getConfigFile ().getString (sPrefix + "proxyHost");
    final int nProxyPort = PDClientConfiguration.getConfigFile ().getInt (sPrefix + "proxyPort", 0);
    if (sProxyHost != null && nProxyPort > 0)
      aClient.setProxy (new HttpHost (sProxyHost, nProxyPort));

    return aClient;
  }
}
