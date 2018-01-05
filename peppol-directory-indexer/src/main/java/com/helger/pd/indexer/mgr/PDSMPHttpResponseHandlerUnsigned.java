/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
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
package com.helger.pd.indexer.mgr;

import java.io.IOException;
import java.nio.charset.Charset;

import javax.annotation.Nonnull;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.entity.ContentType;

import com.helger.commons.io.stream.StreamHelper;
import com.helger.pd.businesscard.v1.PD1BusinessCardMarshaller;
import com.helger.pd.businesscard.v1.PD1BusinessCardType;
import com.helger.pd.businesscard.v2.PD2BusinessCardMarshaller;
import com.helger.pd.businesscard.v2.PD2BusinessCardType;
import com.helger.peppol.httpclient.AbstractSMPResponseHandler;

/**
 * Handle unsigned SMP responses and interpret as PD v1 or v2.
 *
 * @author Philip Helger
 */
public class PDSMPHttpResponseHandlerUnsigned extends AbstractSMPResponseHandler <PD1BusinessCardType>
{
  @Override
  @Nonnull
  public PD1BusinessCardType handleEntity (@Nonnull final HttpEntity aEntity) throws IOException
  {
    // Read the payload
    final ContentType aContentType = ContentType.getOrDefault (aEntity);
    final Charset aCharset = aContentType.getCharset ();
    final byte [] aData = StreamHelper.getAllBytes (aEntity.getContent ());

    // Read version 1
    final PD1BusinessCardMarshaller aMarshaller1 = new PD1BusinessCardMarshaller ();
    if (aCharset != null)
      aMarshaller1.setCharset (aCharset);
    PD1BusinessCardType ret = aMarshaller1.read (aData);
    if (ret == null)
    {
      // Read as version 2
      final PD2BusinessCardMarshaller aMarshaller2 = new PD2BusinessCardMarshaller ();
      if (aCharset != null)
        aMarshaller2.setCharset (aCharset);
      final PD2BusinessCardType aPD2 = aMarshaller2.read (aData);
      if (aPD2 != null)
        ret = PD2BusinessCardMarshaller.getAsV1 (aPD2);
    }
    if (ret == null)
      throw new ClientProtocolException ("Malformed XML document returned from SMP server");
    return ret;
  }
}
