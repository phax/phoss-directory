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
package com.helger.pd.businesscard.helper;

import java.nio.charset.Charset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.w3c.dom.Node;

import com.helger.commons.ValueEnforcer;
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

/**
 * Helper class for business cards.
 *
 * @author Philip Helger
 */
@Immutable
public final class PDBusinessCardHelper
{
  private PDBusinessCardHelper ()
  {}

  /**
   * A generic reading API to read all supported versions of the BusinessCard
   * from a byte array and an optional character set.
   *
   * @param aData
   *        Bytes to read. May not be <code>null</code>.
   * @param aCharset
   *        Character set to use. May be <code>null</code> in which case the XML
   *        character set determination takes place.
   * @return <code>null</code> if parsing fails.
   */
  @Nullable
  public static PDBusinessCard parseBusinessCard (@Nonnull final byte [] aData, @Nullable final Charset aCharset)
  {
    ValueEnforcer.notNull (aData, "Data");

    {
      // Read version 1
      final PD1BusinessCardMarshaller aMarshaller1 = new PD1BusinessCardMarshaller ();
      aMarshaller1.readExceptionCallbacks ().removeAll ();
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
      aMarshaller2.readExceptionCallbacks ().removeAll ();
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
      aMarshaller3.readExceptionCallbacks ().removeAll ();
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

    // Unsupported version
    return null;
  }

  /**
   * A generic reading API to read all supported versions of the BusinessCard
   * from a DOM node.
   *
   * @param aNode
   *        Pre-parsed XML node to read. May not be <code>null</code>.
   * @return <code>null</code> if parsing fails.
   * @since 0.7.2
   */
  @Nullable
  public static PDBusinessCard parseBusinessCard (@Nonnull final Node aNode)
  {
    ValueEnforcer.notNull (aNode, "Node");

    {
      // Read version 1
      final PD1BusinessCardMarshaller aMarshaller1 = new PD1BusinessCardMarshaller ();
      aMarshaller1.readExceptionCallbacks ().removeAll ();
      final PD1BusinessCardType aBC1 = aMarshaller1.read (aNode);
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
      aMarshaller2.readExceptionCallbacks ().removeAll ();
      final PD2BusinessCardType aBC2 = aMarshaller2.read (aNode);
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
      aMarshaller3.readExceptionCallbacks ().removeAll ();
      final PD3BusinessCardType aBC3 = aMarshaller3.read (aNode);
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

    // Unsupported version
    return null;
  }
}
