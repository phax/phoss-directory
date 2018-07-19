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
package com.helger.pd.publisher.servlet;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.io.file.FilenameHelper;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.state.EContinue;
import com.helger.pd.publisher.exportall.ExportAllManager;
import com.helger.photon.core.servlet.AbstractObjectDeliveryHttpHandler;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

public class ExportDeliveryHttpHandler extends AbstractObjectDeliveryHttpHandler
{
  public static final String SPECIAL_BUSINESS_CARDS = "/businesscards";
  private static final Logger LOGGER = LoggerFactory.getLogger (ExportDeliveryHttpHandler.class);

  @Nonnull
  private static String _getBundleIDFromFilename (@Nonnull final String sFilename)
  {
    // Cut leading path ("/") and file extension
    return FilenameHelper.getBaseName (sFilename);
  }

  @Override
  @OverridingMethodsMustInvokeSuper
  public EContinue initRequestState (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                                     @Nonnull final UnifiedResponse aUnifiedResponse)
  {
    if (super.initRequestState (aRequestScope, aUnifiedResponse).isBreak ())
      return EContinue.BREAK;

    // Allow only valid filenames
    final String sFilename = aRequestScope.attrs ().getAsString (REQUEST_ATTR_OBJECT_DELIVERY_FILENAME);
    if (!sFilename.equals (SPECIAL_BUSINESS_CARDS))
    {
      LOGGER.warn ("Cannot special stream the resource '" + sFilename + "'");
      aUnifiedResponse.setStatus (HttpServletResponse.SC_NOT_FOUND);
      return EContinue.BREAK;
    }
    return EContinue.CONTINUE;
  }

  @Override
  protected void onDeliverResource (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                                    @Nonnull final UnifiedResponse aUnifiedResponse,
                                    @Nonnull final String sFilename) throws IOException
  {
    if (sFilename.equals (SPECIAL_BUSINESS_CARDS))
    {
      aUnifiedResponse.disableCaching ();
      ExportAllManager.streamTo (aUnifiedResponse);
      aUnifiedResponse.setMimeType (CMimeType.APPLICATION_XML);
      aUnifiedResponse.setContentDispositionFilename ("directory-export-business-cards.xml");
    }
    else
    {
      throw new IllegalStateException ("Unexpected '" + sFilename + "'");
    }
  }
}
