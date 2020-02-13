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

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.OverrideOnDemand;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.state.ESuccess;
import com.helger.commons.url.URLHelper;
import com.helger.httpclient.HttpClientManager;
import com.helger.peppolid.IParticipantIdentifier;

/**
 * This class is used for calling the PD indexer REST interface.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class PDClient implements Closeable
{
  /** The fixed part of the URL to the PD server */
  public static final String PATH_INDEXER_10 = "indexer/1.0/";

  private static final Logger LOGGER = LoggerFactory.getLogger (PDClient.class);

  @Nonnull
  private static IPDClientExceptionCallback _createDefaultExCb ()
  {
    return (p, m, t) -> LOGGER.error ("Internal error in " + m + " for " + p.getURIEncoded (), t);
  }

  /**
   * The string representation of the Peppol Directory host URL, always ending
   * with a trailing slash!
   */
  private final String m_sPDHost;
  private final String m_sPDIndexerURI;
  private IPDClientExceptionCallback m_aExceptionHdl = _createDefaultExCb ();

  // Important to use the PDHttpClientFactory
  private HttpClientManager m_aHttpClientMgr;

  /**
   * Constructor with a direct Peppol Directory URL.
   *
   * @param sPDHost
   *        The address of the Peppol Directory Server including the application
   *        server context path but without the REST interface. May be http or
   *        https. Example: https://directory.peppol.eu/
   */
  public PDClient (@Nonnull final String sPDHost)
  {
    this (URLHelper.getAsURI (sPDHost));
  }

  /**
   * Constructor with a direct Peppol Directory URL.
   *
   * @param aPDHost
   *        The address of the Peppol Directory Server including the application
   *        server context path but without the REST interface. May be http or
   *        https. Example: https://directory.peppol.eu/
   */
  public PDClient (@Nonnull final URI aPDHost)
  {
    ValueEnforcer.notNull (aPDHost, "PDHost");

    // Build string and ensure it ends with a "/"
    final String sSMPHost = aPDHost.toString ();
    m_sPDHost = sSMPHost.endsWith ("/") ? sSMPHost : sSMPHost + '/';
    m_sPDIndexerURI = m_sPDHost + PATH_INDEXER_10;

    // Get proxy settings
    final boolean bIsHttps = m_sPDHost.startsWith ("https:");
    m_aHttpClientMgr = new HttpClientManager (new PDHttpClientFactory (bIsHttps));
  }

  public void close ()
  {
    StreamHelper.close (m_aHttpClientMgr);
  }

  /**
   * Get the current installed exception handler. By default a logging handler
   * is installed.
   *
   * @return The exception handler currently in place. Never <code>null</code>.
   * @see #setExceptionHandler(IPDClientExceptionCallback)
   * @since 0.5.1
   */
  @Nonnull
  public final IPDClientExceptionCallback getExceptionHandler ()
  {
    return m_aExceptionHdl;
  }

  /**
   * Set the exception handler to be used. It is invoked for every HTTP request
   * that is performed and which throws an exception. The most common exception
   * type is an {@link org.apache.http.client.HttpResponseException} indicating
   * that something went wrong with an HTTP request.
   *
   * @param aExceptionHdl
   *        The exception callback to be invoked. May not be <code>null</code>.
   * @see #getExceptionHandler()
   * @since 0.5.1
   */
  public final void setExceptionHandler (@Nonnull final IPDClientExceptionCallback aExceptionHdl)
  {
    ValueEnforcer.notNull (aExceptionHdl, "ExceptionHdl");
    m_aExceptionHdl = aExceptionHdl;
  }

  /**
   * @return The Peppol Directory host URI string we're operating on. Never
   *         <code>null</code>. Always has a trailing "/".
   */
  @Nonnull
  public final String getPDHostURI ()
  {
    return m_sPDHost;
  }

  /**
   * @return The Peppol Directory indexer URL to use. Never <code>null</code>.
   *         Always has a trailing "/".
   * @since 0.8.5
   */
  @Nonnull
  public final String getPDIndexerURI ()
  {
    return m_sPDIndexerURI;
  }

  /**
   * @return The internal HTTP client manager. Don't mess with it.
   * @since 0.8.5
   */
  @Nonnull
  public final HttpClientManager getHttpClientManager ()
  {
    return m_aHttpClientMgr;
  }

  /**
   * Internal method to set a different {@link HttpClientManager} in case the
   * default one using {@link PDHttpClientFactory} is not suitable (any more).
   *
   * @param aHttpClientMgr
   *        The new HTTP client manager to use. May not be <code>null</code>.
   */
  public final void setHttpClientManager (@Nonnull final HttpClientManager aHttpClientMgr)
  {
    ValueEnforcer.notNull (aHttpClientMgr, "HttpClientMgr");
    m_aHttpClientMgr = aHttpClientMgr;
  }

  /**
   * The main execution routine. Overwrite this method to add additional
   * properties to the call.
   *
   * @param aRequest
   *        The request to be executed. Never <code>null</code>.
   * @param aHandler
   *        The response handler to be used. May not be <code>null</code>.
   * @return The return value of the response handler. Never <code>null</code>.
   * @throws IOException
   *         On HTTP error
   * @param <T>
   *        Response type
   */
  @Nonnull
  @OverrideOnDemand
  protected <T> T executeRequest (@Nonnull final HttpRequestBase aRequest,
                                  @Nonnull final ResponseHandler <T> aHandler) throws IOException
  {
    return m_aHttpClientMgr.execute (aRequest, aHandler);
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

    final HttpGet aGet = new HttpGet (m_sPDIndexerURI + aParticipantID.getURIPercentEncoded ());
    try
    {
      return executeRequest (aGet, new PDClientResponseHandler ()).isSuccess ();
    }
    catch (final Throwable t)
    {
      m_aExceptionHdl.onException (aParticipantID, "isServiceGroupRegistered", t);
    }
    return false;
  }

  @Nonnull
  public ESuccess addServiceGroupToIndex (@Nonnull final IParticipantIdentifier aParticipantID)
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    final String sParticipantID = aParticipantID.getURIEncoded ();

    final HttpPut aPut = new HttpPut (m_sPDIndexerURI);
    aPut.setEntity (new StringEntity (sParticipantID, StandardCharsets.UTF_8));

    try
    {
      if (executeRequest (aPut, new PDClientResponseHandler ()).isSuccess ())
      {
        LOGGER.info ("Added service group '" +
                     sParticipantID +
                     "' to Peppol Directory index. May take some time until it shows up.");
        return ESuccess.SUCCESS;
      }
    }
    catch (final Throwable t)
    {
      m_aExceptionHdl.onException (aParticipantID, "addServiceGroupToIndex", t);
    }
    return ESuccess.FAILURE;
  }

  @Nonnull
  public ESuccess deleteServiceGroupFromIndex (@Nonnull final IParticipantIdentifier aParticipantID)
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");

    final HttpDelete aDelete = new HttpDelete (m_sPDIndexerURI + aParticipantID.getURIPercentEncoded ());
    try
    {
      if (executeRequest (aDelete, new PDClientResponseHandler ()).isSuccess ())
      {
        final String sParticipantID = aParticipantID.getURIEncoded ();
        LOGGER.info ("Removed service group '" +
                     sParticipantID +
                     "' from Peppol Directory index. May take some time until it is removed.");
        return ESuccess.SUCCESS;
      }
    }
    catch (final Throwable t)
    {
      m_aExceptionHdl.onException (aParticipantID, "deleteServiceGroupFromIndex", t);
    }
    return ESuccess.FAILURE;
  }
}
