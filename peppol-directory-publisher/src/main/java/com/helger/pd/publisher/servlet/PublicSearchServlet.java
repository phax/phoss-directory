/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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

import java.nio.charset.StandardCharsets;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.ReturnsImmutableObject;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.string.StringHelper;
import com.helger.http.EHTTPMethod;
import com.helger.pd.publisher.search.EPDOutputFormat;
import com.helger.photon.core.servlet.AbstractUnifiedResponseServlet;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * The REST search servlet. Handles only GET requests.
 *
 * @author Philip Helger
 */
public final class PublicSearchServlet extends AbstractUnifiedResponseServlet
{
  private static final String VERSION1_PREFIX = "/1.0";
  private static final Logger s_aLogger = LoggerFactory.getLogger (PublicSearchServlet.class);

  @Override
  @Nonnull
  @ReturnsImmutableObject
  protected Set <EHTTPMethod> getAllowedHTTPMethods ()
  {
    // Only GET is allowed
    return ALLOWED_METHDOS_GET;
  }

  @Override
  protected void handleRequest (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                                @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    // http://127.0.0.1:8080/search -> null
    // http://127.0.0.1:8080/search/ -> "/"
    final String sPathInfo = StringHelper.getNotNull (aRequestScope.getPathInfo (), "");
    if (StringHelper.startsWith (sPathInfo, VERSION1_PREFIX))
    {
      // Version 1.0

      // Determine output format
      final ICommonsList <String> aParts = StringHelper.getExploded ('/', sPathInfo.substring (1), 2);
      final String sFormat = aParts.getAtIndex (1);
      EPDOutputFormat eOutputFormat = EPDOutputFormat.getFromIDCaseInsensitiveOrNull (sFormat);
      if (eOutputFormat == null)
      {
        // Defaults to XML
        eOutputFormat = EPDOutputFormat.XML;
      }
      s_aLogger.info ("Using REST API 1.0 with output format " + eOutputFormat + " (" + sPathInfo + ")");

      // TODO parse search term

      aUnifiedResponse.setContentAndCharset ("<something-will-be-here-soon be=\"patient\" />", StandardCharsets.UTF_8);
    }
    else
    {
      s_aLogger.error ("Unsupported version provided (" + sPathInfo + ")");
      aUnifiedResponse.setStatus (HttpServletResponse.SC_NOT_FOUND);
    }
  }
}
