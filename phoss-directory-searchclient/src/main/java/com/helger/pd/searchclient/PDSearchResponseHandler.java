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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.HttpResponseException;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.base.string.StringParser;
import com.helger.http.CHttp;
import com.helger.http.CHttpHeader;
import com.helger.httpclient.HttpClientHelper;
import com.helger.httpclient.response.ExtendedHttpResponseException;
import com.helger.pd.searchapi.PDResultListMarshaller;
import com.helger.pd.searchapi.v1.ResultListType;

/**
 * Special response handler for the PD search API. Unmarshals a successful response into a
 * {@link ResultListType} using {@link PDResultListMarshaller}, which validates it against the
 * shipped XML Schema.<br>
 * A query without a single match is answered with HTTP 200 and an empty result list, so an empty
 * result is a regular success and never an error.<br>
 * HTTP 429 is turned into a {@link PDSearchRateLimitException} carrying the "Retry-After" value, so
 * that a caller can back off. Every other unexpected status code - including HTTP 404, that the
 * server only uses for an unsupported API version path - results in an
 * {@link HttpResponseException}.
 *
 * @author Philip Helger
 * @since 0.19.1
 */
public class PDSearchResponseHandler implements HttpClientResponseHandler <ResultListType>
{
  public PDSearchResponseHandler ()
  {}

  @Nullable
  private static String _getEntityAsString (@Nullable final HttpEntity aEntity) throws IOException
  {
    if (aEntity == null)
      return null;

    final ContentType aContentType = HttpClientHelper.getContentTypeOrDefault (aEntity, ContentType.DEFAULT_TEXT);

    // Default to UTF-8 internally
    Charset aCharset = aContentType.getCharset ();
    if (aCharset == null)
      aCharset = StandardCharsets.UTF_8;

    return HttpClientHelper.entityToString (aEntity, aCharset);
  }

  @NonNull
  public ResultListType handleResponse (@NonNull final ClassicHttpResponse aHttpResponse) throws HttpException, IOException
  {
    final int nCode = aHttpResponse.getCode ();

    // Success
    if (nCode >= 200 && nCode < 300)
    {
      final String sContent = _getEntityAsString (aHttpResponse.getEntity ());
      if (sContent == null)
        throw new ClientProtocolException ("The PD search API returned HTTP " + nCode + " but no response body");

      final ResultListType aResultList = new PDResultListMarshaller ().read (sContent);
      if (aResultList == null)
        throw new ClientProtocolException ("Failed to parse the PD search API response as a valid Result List v1 document");
      return aResultList;
    }

    // Rate limit exceeded - tell the caller how long to wait
    if (nCode == CHttp.HTTP_TOO_MANY_REQUESTS)
    {
      final Header aRetryAfter = aHttpResponse.getFirstHeader (CHttpHeader.RETRY_AFTER);
      final int nRetryAfterSeconds = aRetryAfter == null ? -1 : StringParser.parseInt (aRetryAfter.getValue (), -1);
      throw new PDSearchRateLimitException ("The PD search API rejected the request because the rate limit was exceeded" +
                                            (nRetryAfterSeconds >= 0 ? " - retry after " +
                                                                       nRetryAfterSeconds +
                                                                       " seconds" : ""),
                                            nRetryAfterSeconds);
    }

    // Unexpected response code
    throw ExtendedHttpResponseException.create (aHttpResponse);
  }
}
