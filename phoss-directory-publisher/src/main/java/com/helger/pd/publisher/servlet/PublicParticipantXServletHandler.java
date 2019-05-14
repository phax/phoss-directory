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
package com.helger.pd.publisher.servlet;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.SimpleURL;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.publisher.app.AppCommonUI;
import com.helger.pd.publisher.app.pub.CMenuPublic;
import com.helger.pd.publisher.app.pub.page.PagePublicSearchSimple;
import com.helger.peppol.identifier.factory.PeppolIdentifierFactory;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.photon.basic.app.request.RequestParameterManager;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xservlet.handler.simple.IXServletSimpleHandler;

/**
 * The participant quick lookup servlet (issue #30). Handles only GET requests.
 *
 * @author Philip Helger
 */
public final class PublicParticipantXServletHandler implements IXServletSimpleHandler
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PublicParticipantXServletHandler.class);

  public void handleRequest (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                             @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    // http://127.0.0.1:8080/participant -> null
    // http://127.0.0.1:8080/participant/ -> "/"
    // http://127.0.0.1:8080/participant/x -> "/x"
    final String sPathInfo = StringHelper.getNotNull (aRequestScope.getPathInfo (), "");
    final ICommonsList <String> aParts = StringHelper.getExploded ('/', sPathInfo.substring (1));

    IParticipantIdentifier aPI = null;
    if (aParts.isNotEmpty ())
    {
      final String sID = aParts.get (0);
      aPI = PDMetaManager.getIdentifierFactory ().parseParticipantIdentifier (sID);
      if (aPI == null)
      {
        // Maybe the scheme is missing
        // Check if there is a second part as in
        // /participant/iso6523-actorid-upis/9915:test
        if (aParts.size () >= 2)
        {
          final String sScheme = sID;
          final String sValue = aParts.get (1);
          aPI = PDMetaManager.getIdentifierFactory ().createParticipantIdentifier (sScheme, sValue);
        }
      }

      if (aPI == null)
      {
        // Still failure - try PEPPOL default scheme
        aPI = PDMetaManager.getIdentifierFactory ()
                           .createParticipantIdentifier (PeppolIdentifierFactory.INSTANCE.getDefaultParticipantIdentifierScheme (),
                                                         sID);
      }
    }

    if (aPI == null)
    {
      LOGGER.error ("Failed to resolve path '" + sPathInfo + "' to a participant ID!");
      aUnifiedResponse.setStatus (HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    // Redirect to search result page
    final SimpleURL aTarget = RequestParameterManager.getInstance ()
                                                     .getLinkToMenuItem (aRequestScope,
                                                                         AppCommonUI.DEFAULT_LOCALE,
                                                                         CMenuPublic.MENU_SEARCH_SIMPLE)
                                                     .add (CPageParam.PARAM_ACTION, CPageParam.ACTION_VIEW)
                                                     .add (PagePublicSearchSimple.FIELD_QUERY, aPI.getURIEncoded ())
                                                     .add (PagePublicSearchSimple.FIELD_PARTICIPANT_ID,
                                                           aPI.getURIEncoded ());
    aUnifiedResponse.setRedirect (aTarget);
  }
}
