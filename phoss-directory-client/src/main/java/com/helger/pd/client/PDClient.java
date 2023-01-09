/*
 * Copyright (C) 2015-2023 Philip Helger (www.helger.com)
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

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.StringEntity;
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
 * This class is used for calling the PD indexer REST interface. The only part
 * that concerns the configuration file is in the {@link PDHttpClientSettings}
 * used to customize the HTTP connectivity.
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
    return (p, m, t) -> {
      if (LOGGER.isErrorEnabled ())
        LOGGER.error ("Internal error in " + m + " for " + p.getURIEncoded (), t);
    };
  }

  /**
   * The string representation of the Peppol Directory host URL, always ending
   * with a trailing slash!
   */
  private final String m_sPDHostURI;
  private final String m_sPDIndexerURI;
  private IPDClientExceptionCallback m_aExceptionHdl = _createDefaultExCb ();

  // Important to use the PDHttpClientSettings internally
  private HttpClientManager m_aHttpClientMgr;

  /**
   * Constructor with a direct Peppol Directory URL.
   *
   * @param sPDHost
   *        The address of the Peppol Directory Server including the application
   *        server context path but without the REST interface. May be http or
   *        https. Example: https://directory.peppol.eu/
   * @throws IllegalStateException
   *         If the "https" protocol is used, and the SSL setup is incomplete.
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
   * @throws IllegalStateException
   *         If the "https" protocol is used, and the SSL setup is incomplete.
   */
  public PDClient (@Nonnull final URI aPDHost)
  {
    ValueEnforcer.notNull (aPDHost, "PDHost");

    // Build string and ensure it ends with a "/"
    final String sSMPHost = aPDHost.toString ();
    m_sPDHostURI = sSMPHost.endsWith ("/") ? sSMPHost : sSMPHost + '/';
    m_sPDIndexerURI = m_sPDHostURI + PATH_INDEXER_10;
    m_aHttpClientMgr = HttpClientManager.create (new PDHttpClientSettings (m_sPDHostURI));
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
   * type is an {@link org.apache.hc.client5.http.HttpResponseException}
   * indicating that something went wrong with an HTTP request.
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
    return m_sPDHostURI;
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
   * default one using {@link PDHttpClientSettings} is not suitable (any more).
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
  protected <T> T executeRequest (@Nonnull final HttpUriRequest aRequest,
                                  @Nonnull final HttpClientResponseHandler <T> aHandler) throws IOException
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
    catch (final Exception ex)
    {
      m_aExceptionHdl.onException (aParticipantID, "isServiceGroupRegistered", ex);
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
        if (LOGGER.isInfoEnabled ())
          LOGGER.info ("Added service group '" +
                       sParticipantID +
                       "' to Peppol Directory index. May take some time until it shows up.");
        return ESuccess.SUCCESS;
      }
    }
    catch (final Exception ex)
    {
      m_aExceptionHdl.onException (aParticipantID, "addServiceGroupToIndex", ex);
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
        if (LOGGER.isInfoEnabled ())
          LOGGER.info ("Removed service group '" +
                       sParticipantID +
                       "' from Peppol Directory index. May take some time until it is removed.");
        return ESuccess.SUCCESS;
      }
    }
    catch (final Exception ex)
    {
      m_aExceptionHdl.onException (aParticipantID, "deleteServiceGroupFromIndex", ex);
    }
    return ESuccess.FAILURE;
  }
}
