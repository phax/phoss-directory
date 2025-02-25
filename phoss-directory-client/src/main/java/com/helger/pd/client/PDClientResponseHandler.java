/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.HttpResponseException;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;

import com.helger.commons.state.ESuccess;
import com.helger.httpclient.HttpClientHelper;

/**
 * Special response handler for PD client
 *
 * @author Philip Helger
 */
public class PDClientResponseHandler implements HttpClientResponseHandler <ESuccess>
{
  public PDClientResponseHandler ()
  {}

  @Nullable
  public ESuccess handleResponse (@Nonnull final ClassicHttpResponse aHttpResponse) throws ClientProtocolException,
                                                                                    IOException
  {
    // Check result
    if (aHttpResponse.getCode () >= 200 && aHttpResponse.getCode () < 300)
      return ESuccess.SUCCESS;

    // Not found
    if (aHttpResponse.getCode () == 404)
      return ESuccess.FAILURE;

    // Unexpected
    final HttpEntity aEntity = aHttpResponse.getEntity ();
    String sContent = null;
    if (aEntity != null)
    {
      final ContentType aContentType = HttpClientHelper.getContentTypeOrDefault (aEntity, ContentType.DEFAULT_TEXT);

      // Default to UTF-8 internally
      Charset aCharset = aContentType.getCharset ();
      if (aCharset == null)
        aCharset = StandardCharsets.UTF_8;

      sContent = HttpClientHelper.entityToString (aEntity, aCharset);
    }

    String sMessage = aHttpResponse.getReasonPhrase () + " [" + aHttpResponse.getCode () + "]";
    if (sContent != null)
      sMessage += "\nResponse content: " + sContent;
    throw new HttpResponseException (aHttpResponse.getCode (), sMessage);
  }
}
