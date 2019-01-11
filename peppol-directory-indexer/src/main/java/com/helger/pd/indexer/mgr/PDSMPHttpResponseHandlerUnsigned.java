/**
 * Copyright (C) 2015-2019 Philip Helger (www.helger.com)
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
import javax.annotation.Nullable;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.entity.ContentType;

import com.helger.commons.io.stream.StreamHelper;
import com.helger.pd.businesscard.generic.PDBusinessCard;
import com.helger.pd.businesscard.v1.PD1APIHelper;
import com.helger.pd.businesscard.v1.PD1BusinessCardMarshaller;
import com.helger.pd.businesscard.v1.PD1BusinessCardType;
import com.helger.pd.businesscard.v2.PD2APIHelper;
import com.helger.pd.businesscard.v2.PD2BusinessCardMarshaller;
import com.helger.pd.businesscard.v2.PD2BusinessCardType;
import com.helger.pd.businesscard.v3.PD3APIHelper;
import com.helger.pd.businesscard.v3.PD3BusinessCardMarshaller;
import com.helger.pd.businesscard.v3.PD3BusinessCardType;
import com.helger.peppol.httpclient.AbstractSMPResponseHandler;

/**
 * Handle unsigned SMP responses and interpret as PD v1 or v2 or v3.
 *
 * @author Philip Helger
 */
public class PDSMPHttpResponseHandlerUnsigned extends AbstractSMPResponseHandler <PDBusinessCard>
{
  @Override
  @Nullable
  public PDBusinessCard handleEntity (@Nonnull final HttpEntity aEntity) throws IOException
  {
    // Read the payload and remember it!
    final ContentType aContentType = ContentType.getOrDefault (aEntity);
    final Charset aCharset = aContentType.getCharset ();
    final byte [] aData = StreamHelper.getAllBytes (aEntity.getContent ());

    {
      // Read version 1
      final PD1BusinessCardMarshaller aMarshaller1 = new PD1BusinessCardMarshaller ();
      if (aCharset != null)
        aMarshaller1.setCharset (aCharset);
      final PD1BusinessCardType aBC1 = aMarshaller1.read (aData);
      if (aBC1 != null)
        try
        {
          return PD1APIHelper.createBusinessCard (aBC1);
        }
        catch (final IllegalArgumentException ex)
        {
          // If the BC does not adhere to the XSD
          // Happens if e.g. name is null
          return null;
        }
    }

    {
      // Read as version 2
      final PD2BusinessCardMarshaller aMarshaller2 = new PD2BusinessCardMarshaller ();
      if (aCharset != null)
        aMarshaller2.setCharset (aCharset);
      final PD2BusinessCardType aBC2 = aMarshaller2.read (aData);
      if (aBC2 != null)
        try
        {
          return PD2APIHelper.createBusinessCard (aBC2);
        }
        catch (final IllegalArgumentException ex)
        {
          // If the BC does not adhere to the XSD
          // Happens if e.g. name is null
          return null;
        }
    }

    {
      // Read as version 3
      final PD3BusinessCardMarshaller aMarshaller3 = new PD3BusinessCardMarshaller ();
      if (aCharset != null)
        aMarshaller3.setCharset (aCharset);
      final PD3BusinessCardType aBC3 = aMarshaller3.read (aData);
      if (aBC3 != null)
        try
        {
          return PD3APIHelper.createBusinessCard (aBC3);
        }
        catch (final IllegalArgumentException ex)
        {
          // If the BC does not adhere to the XSD
          // Happens if e.g. name is null
          return null;
        }
    }

    // Unsupported
    throw new ClientProtocolException ("Malformed XML document returned from SMP server");
  }
}
