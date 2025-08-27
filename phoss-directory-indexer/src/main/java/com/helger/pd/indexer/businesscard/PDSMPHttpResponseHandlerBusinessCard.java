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
package com.helger.pd.indexer.businesscard;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;

import com.helger.base.io.stream.StreamHelper;
import com.helger.httpclient.HttpClientHelper;
import com.helger.peppol.businesscard.generic.PDBusinessCard;
import com.helger.peppol.businesscard.helper.PDBusinessCardHelper;
import com.helger.smpclient.httpclient.AbstractSMPResponseHandler;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Handle unsigned SMP responses and interpret as PD v1 or v2 or v3.
 *
 * @author Philip Helger
 */
final class PDSMPHttpResponseHandlerBusinessCard extends AbstractSMPResponseHandler <PDBusinessCard>
{
  @Override
  @Nullable
  public PDBusinessCard handleEntity (@Nonnull final HttpEntity aEntity) throws IOException
  {
    // Read the payload and remember it!
    final ContentType aContentType = HttpClientHelper.getContentTypeOrDefault (aEntity);
    final Charset aCharset = aContentType.getCharset ();
    final byte [] aData = StreamHelper.getAllBytes (aEntity.getContent ());
    if (aData == null)
      return null;

    final PDBusinessCard aBC = PDBusinessCardHelper.parseBusinessCard (aData, aCharset);
    if (aBC != null)
      return aBC;

    // Unsupported
    throw new ClientProtocolException ("Malformed XML document returned from SMP server (no supported BusinessCard format):\n" +
                                       new String (aData, StandardCharsets.UTF_8));
  }
}
