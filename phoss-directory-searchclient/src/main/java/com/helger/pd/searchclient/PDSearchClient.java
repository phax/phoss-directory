/*
 * Copyright (C) 2026 Philip Helger (www.helger.com)
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
package com.helger.pd.searchclient;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.NotThreadSafe;
import com.helger.annotation.style.OverrideOnDemand;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.io.stream.StreamHelper;
import com.helger.base.url.URLHelper;
import com.helger.httpclient.HttpClientManager;
import com.helger.httpclient.HttpClientSettings;
import com.helger.pd.searchapi.CPDSearchAPI;
import com.helger.pd.searchapi.v1.ResultListType;

/**
 * This class is used for calling the PD search REST interface. Contrary to
 * <code>PDClient</code> of the "phoss-directory-client" submodule, that pushes indexing requests
 * and therefore needs an SMP client certificate, the search API is publicly readable and needs no
 * authentication at all.
 *
 * @author Philip Helger
 * @since 0.19.1
 */
@NotThreadSafe
public class PDSearchClient implements Closeable
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PDSearchClient.class);

  /**
   * The string representation of the Peppol Directory host URL, always ending with a trailing
   * slash!
   */
  private final String m_sPDHostURI;
  private final String m_sPDSearchURI;

  private HttpClientManager m_aHttpClientMgr;

  /**
   * Constructor with a direct Peppol Directory URL.
   *
   * @param sPDHost
   *        The address of the Peppol Directory Server including the application server context path
   *        but without the REST interface. May be http or https. Example:
   *        https://directory.peppol.eu/
   */
  public PDSearchClient (@NonNull final String sPDHost)
  {
    this (URLHelper.getAsURI (sPDHost));
  }

  /**
   * Constructor with a direct Peppol Directory URL, using the default HTTP client settings.
   *
   * @param aPDHost
   *        The address of the Peppol Directory Server including the application server context path
   *        but without the REST interface. May be http or https. Example:
   *        https://directory.peppol.eu/
   */
  public PDSearchClient (@NonNull final URI aPDHost)
  {
    this (aPDHost, new HttpClientSettings ());
  }

  /**
   * Constructor with a direct Peppol Directory URL and customized HTTP client settings.
   *
   * @param aPDHost
   *        The address of the Peppol Directory Server including the application server context path
   *        but without the REST interface. May be http or https. Example:
   *        https://directory.peppol.eu/
   * @param aHCS
   *        The HTTP client settings to be used - use this to configure a proxy server, the timeouts
   *        or a custom SSL context. May not be <code>null</code>.
   */
  public PDSearchClient (@NonNull final URI aPDHost, @NonNull final HttpClientSettings aHCS)
  {
    ValueEnforcer.notNull (aPDHost, "PDHost");
    ValueEnforcer.notNull (aHCS, "HttpClientSettings");

    // Build string and ensure it ends with a "/"
    final String sPDHost = aPDHost.toString ();
    m_sPDHostURI = sPDHost.endsWith ("/") ? sPDHost : sPDHost + '/';
    m_sPDSearchURI = m_sPDHostURI + CPDSearchAPI.PATH_SEARCH_10 + CPDSearchAPI.OUTPUT_FORMAT_XML;
    m_aHttpClientMgr = HttpClientManager.create (aHCS);
  }

  public void close ()
  {
    StreamHelper.close (m_aHttpClientMgr);
  }

  /**
   * @return The Peppol Directory host URI string we're operating on. Never <code>null</code>.
   *         Always has a trailing "/".
   */
  @NonNull
  public final String getPDHostURI ()
  {
    return m_sPDHostURI;
  }

  /**
   * @return The Peppol Directory search URL to use, including the requested output format. Never
   *         <code>null</code>. Has no trailing "/".
   */
  @NonNull
  public final String getPDSearchURI ()
  {
    return m_sPDSearchURI;
  }

  /**
   * @return The internal HTTP client manager. Don't mess with it.
   */
  @NonNull
  public final HttpClientManager getHttpClientManager ()
  {
    return m_aHttpClientMgr;
  }

  /**
   * Internal method to set a different {@link HttpClientManager} in case the one created from the
   * {@link HttpClientSettings} of the constructor is not suitable (any more).
   *
   * @param aHttpClientMgr
   *        The new HTTP client manager to use. May not be <code>null</code>.
   */
  public final void setHttpClientManager (@NonNull final HttpClientManager aHttpClientMgr)
  {
    ValueEnforcer.notNull (aHttpClientMgr, "HttpClientMgr");
    m_aHttpClientMgr = aHttpClientMgr;
  }

  /**
   * Build the full URL of a single search query, so that it can be logged or issued by a different
   * HTTP layer.
   *
   * @param aQuery
   *        The query to be executed. May not be <code>null</code> and must carry at least one query
   *        term.
   * @return The absolute URL of the query, including the query string. Never <code>null</code> nor
   *         empty.
   */
  @NonNull
  @Nonempty
  public String getSearchURL (@NonNull final PDSearchQuery aQuery)
  {
    ValueEnforcer.notNull (aQuery, "Query");
    ValueEnforcer.isTrue (aQuery.hasQueryTerms (), "The query must contain at least one query term");

    return m_sPDSearchURI + '?' + aQuery.getAsQueryString ();
  }

  /**
   * The main execution routine. Overwrite this method to add additional properties to the call.
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
  @NonNull
  @OverrideOnDemand
  protected <T> T executeRequest (@NonNull final HttpGet aRequest,
                                  @NonNull final HttpClientResponseHandler <T> aHandler) throws IOException
  {
    return m_aHttpClientMgr.execute (aRequest, aHandler);
  }

  /**
   * Execute a single search query. A query that matches nothing is not an error - it results in a
   * result list with a "total-result-count" of 0.
   *
   * @param aQuery
   *        The query to be executed. May not be <code>null</code> and must carry at least one query
   *        term, because the server rejects a query without one with HTTP 400.
   * @return The parsed result list. Never <code>null</code>.
   * @throws PDSearchRateLimitException
   *         If the server side rate limit was exceeded (HTTP 429). It carries the number of seconds
   *         to wait before retrying.
   * @throws org.apache.hc.client5.http.HttpResponseException
   *         If the server responded with any other unexpected status code.
   * @throws IOException
   *         On HTTP error
   */
  @NonNull
  public ResultListType search (@NonNull final PDSearchQuery aQuery) throws IOException
  {
    // Fail early instead of letting the server answer with HTTP 400
    ValueEnforcer.isTrue (!aQuery.isBeyondMaxResults (),
                          () -> "The query asks for results beyond the maximum result index of " +
                                CPDSearchAPI.MAX_RESULTS +
                                " and would be rejected by the server");

    final String sURL = getSearchURL (aQuery);
    final HttpGet aGet = new HttpGet (sURL);
    aGet.setAbsoluteRequestUri (true);
    LOGGER.info ("PD search@" + sURL);

    return executeRequest (aGet, new PDSearchResponseHandler ());
  }
}
