/**
 * Copyright (C) 2015-2021 Philip Helger (www.helger.com)
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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;

import com.helger.commons.state.ESuccess;

/**
 * Special response handler for PD client
 *
 * @author Philip Helger
 */
public class PDClientResponseHandler implements ResponseHandler <ESuccess>
{
  public PDClientResponseHandler ()
  {}

  @Nullable
  public ESuccess handleResponse (@Nonnull final HttpResponse aHttpResponse) throws ClientProtocolException, IOException
  {
    final StatusLine aStatusLine = aHttpResponse.getStatusLine ();

    // Check result
    if (aStatusLine.getStatusCode () >= 200 && aStatusLine.getStatusCode () < 300)
      return ESuccess.SUCCESS;

    // Not found
    if (aStatusLine.getStatusCode () == 404)
      return ESuccess.FAILURE;

    // Unexpected
    final HttpEntity aEntity = aHttpResponse.getEntity ();
    String sContent = null;
    if (aEntity != null)
    {
      ContentType aContentType = ContentType.get (aEntity);
      if (aContentType == null)
        aContentType = ContentType.DEFAULT_TEXT;

      // Default to UTF-8 internally
      Charset aCharset = aContentType.getCharset ();
      if (aCharset == null)
        aCharset = StandardCharsets.UTF_8;

      sContent = EntityUtils.toString (aEntity, aCharset);
    }

    String sMessage = aStatusLine.getReasonPhrase () + " [" + aStatusLine.getStatusCode () + "]";
    if (sContent != null)
      sMessage += "\nResponse content: " + sContent;
    throw new HttpResponseException (aStatusLine.getStatusCode (), sMessage);
  }
}
