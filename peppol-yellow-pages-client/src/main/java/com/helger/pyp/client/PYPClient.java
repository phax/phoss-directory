/**
 * Copyright (C) 2015 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Version: MPL 1.1/EUPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at:
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * Alternatively, the contents of this file may be used under the
 * terms of the EUPL, Version 1.1 or - as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL
 * (the "Licence"); You may not use this work except in compliance
 * with the Licence.
 * You may obtain a copy of the Licence at:
 * http://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 * If you wish to allow use of your version of this file only
 * under the terms of the EUPL License and not to allow others to use
 * your version of this file under the MPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and
 * other provisions required by the EUPL License. If you do not delete
 * the provisions above, a recipient may use your version of this file
 * under either the MPL or the EUPL License.
 */
package com.helger.pyp.client;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.CodingErrorAction;
import java.security.KeyStore;

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
 * This class is used for calling the PYP indexer REST interface.
 *
 * @author Philip Helger
 */
public class PYPClient implements Closeable
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (PYPClient.class);

  /**
   * The string representation of the PYP host URL, always ending with a
   * trailing slash!
   */
  private final String m_sPYPHost;
  private final String m_sPYPIndexerURL;

  private HttpHost m_aProxy;
  private Credentials m_aProxyCredentials;
  private CloseableHttpClient m_aHttpClient;

  /**
   * Constructor with a direct PYP URL.
   *
   * @param aPYPHost
   *        The address of the PYP server including the application server
   *        context path but without the REST interface. May be http or https.
   *        Example: http://pyp.helger.com/
   */
  public PYPClient (@Nonnull final URI aPYPHost)
  {
    ValueEnforcer.notNull (aPYPHost, "SMPHost");

    // Build string and ensure it ends with a "/"
    final String sSMPHost = aPYPHost.toString ();
    m_sPYPHost = sSMPHost.endsWith ("/") ? sSMPHost : sSMPHost + '/';
    m_sPYPIndexerURL = m_sPYPHost + "indexer/1.0/";
  }

  /**
   * @return The PYP host URI string we're operating on. Never <code>null</code>
   *         . Always has a trailing "/".
   */
  @Nonnull
  public String getPYPHostURI ()
  {
    return m_sPYPHost;
  }

  /**
   * @return The HTTP proxy to be used to access the PYP server. Is
   *         <code>null</code> by default.
   */
  @Nullable
  public HttpHost getProxy ()
  {
    return m_aProxy;
  }

  /**
   * Set the proxy to be used to access the PYP server.
   *
   * @param aProxy
   *        May be <code>null</code> to indicate no proxy.
   */
  public void setProxy (@Nullable final HttpHost aProxy)
  {
    m_aProxy = aProxy;
  }

  /**
   * @return The HTTP proxy credentials to be used to access the PYP server. Is
   *         <code>null</code> by default.
   */
  @Nullable
  public Credentials getProxyCredentials ()
  {
    return m_aProxyCredentials;
  }

  /**
   * Set the proxy Credentials to be used to access the PYP server.
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
      final KeyStore aKeyStore = KeyStoreHelper.loadKeyStore (PYPClientConfiguration.getKeyStorePath (), PYPClientConfiguration.getKeyStorePassword ());
      final SSLContext aSSLContext = SSLContexts.custom ().loadKeyMaterial (aKeyStore, PYPClientConfiguration.getKeyStoreKeyPassword (), (aAliases, aSocket) -> {
        final String sAlias = PYPClientConfiguration.getKeyStoreKeyAlias ();
        return aAliases.containsKey (sAlias) ? sAlias : null;
      }).build ();
      // Allow TLSv1 protocol only
      final SSLConnectionSocketFactory aSSLSocketFactory = new SSLConnectionSocketFactory (aSSLContext, new String [] { "TLSv1" }, null, SSLConnectionSocketFactory.getDefaultHostnameVerifier ());

      final Registry <ConnectionSocketFactory> sfr = RegistryBuilder.<ConnectionSocketFactory> create ()
                                                                    .register ("http", PlainConnectionSocketFactory.getSocketFactory ())
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
    return RequestConfig.custom ().setSocketTimeout (10000).setConnectTimeout (5000).setConnectionRequestTimeout (5000).setProxy (m_aProxy).build ();
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
    final String sResponse = aResponseEntity == null ? null : StreamHelper.getAllBytesAsString (aResponseEntity.getContent (), CCharset.CHARSET_UTF_8_OBJ);
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

    final HttpGet aGet = new HttpGet (m_sPYPIndexerURL + IdentifierHelper.getIdentifierURIPercentEncoded (aParticipantID));
    try (final CloseableHttpResponse aResponse = executeRequest (aGet))
    {
      final String sResponse = _getResponseString (aResponse);

      // Check result
      if (aResponse.getStatusLine ().getStatusCode () >= 200 && aResponse.getStatusLine ().getStatusCode () < 300)
        return true;

      if (aResponse.getStatusLine ().getStatusCode () == 404)
        return false;

      s_aLogger.warn ("Unexpected status returned from server for " + aGet.getRequestLine () + ": " + aResponse.getStatusLine () + "\n" + sResponse);
    }
    catch (final IOException ex)
    {
      s_aLogger.error ("Error performing request " + aGet.getRequestLine (), ex);
    }
    return false;
  }

  @Nonnull
  public ESuccess addServiceGroupToIndex (@Nonnull final IParticipantIdentifier aParticipantID)
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    final String sParticipantID = IdentifierHelper.getIdentifierURIEncoded (aParticipantID);

    final HttpPut aPut = new HttpPut (m_sPYPIndexerURL);
    aPut.setEntity (new StringEntity (sParticipantID, CCharset.CHARSET_UTF_8_OBJ));
    try (final CloseableHttpResponse aResponse = executeRequest (aPut))
    {
      final String sResponse = _getResponseString (aResponse);

      // Check result
      if (aResponse.getStatusLine ().getStatusCode () >= 200 && aResponse.getStatusLine ().getStatusCode () < 300)
      {
        s_aLogger.info ("Added service group '" + sParticipantID + "' to PYP index. May take some time until it shows up.");
        return ESuccess.SUCCESS;
      }

      s_aLogger.warn ("Unexpected status returned from server for " + aPut.getRequestLine () + ": " + aResponse.getStatusLine () + "\n" + sResponse);
    }
    catch (final IOException ex)
    {
      s_aLogger.error ("Error performing request " + aPut.getRequestLine (), ex);
    }
    return ESuccess.FAILURE;
  }

  @Nonnull
  public ESuccess deleteServiceGroupFromIndex (@Nonnull final IParticipantIdentifier aParticipantID)
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");

    final HttpDelete aDelete = new HttpDelete (m_sPYPIndexerURL + IdentifierHelper.getIdentifierURIPercentEncoded (aParticipantID));
    try (final CloseableHttpResponse aResponse = executeRequest (aDelete))
    {
      final String sResponse = _getResponseString (aResponse);

      // Check result
      if (aResponse.getStatusLine ().getStatusCode () >= 200 && aResponse.getStatusLine ().getStatusCode () < 300)
      {
        final String sParticipantID = IdentifierHelper.getIdentifierURIEncoded (aParticipantID);
        s_aLogger.info ("Removed service group '" + sParticipantID + "' from PYP index. May take some time until it is removed.");
        return ESuccess.SUCCESS;
      }

      s_aLogger.warn ("Unexpected status returned from server for " + aDelete.getRequestLine () + ": " + aResponse.getStatusLine () + "\n" + sResponse);
    }
    catch (final IOException ex)
    {
      s_aLogger.error ("Error performing request " + aDelete.getRequestLine (), ex);
    }
    return ESuccess.FAILURE;
  }

  public void close () throws IOException
  {
    if (m_aHttpClient != null)
      m_aHttpClient.close ();
  }

  @Nonnull
  public static PYPClient createDefaultClient ()
  {
    final PYPClient aClient = new PYPClient (URI.create ("http://pyp.helger.com"));
    final boolean bIsHttp = aClient.getPYPHostURI ().startsWith ("http:");

    // Get proxy settings
    final String sPrefix = bIsHttp ? "http." : "https.";
    final String sProxyHost = PYPClientConfiguration.getConfigFile ().getString (sPrefix + "proxyHost");
    final int nProxyPort = PYPClientConfiguration.getConfigFile ().getInt (sPrefix + "proxyPort", 0);
    if (sProxyHost != null && nProxyPort > 0)
      aClient.setProxy (new HttpHost (sProxyHost, nProxyPort));

    return aClient;
  }
}
