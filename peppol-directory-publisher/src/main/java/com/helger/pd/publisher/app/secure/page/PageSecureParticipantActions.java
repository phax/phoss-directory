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
package com.helger.pd.publisher.app.secure.page;

import java.io.IOException;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.ICommonsSortedSet;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.publisher.exportall.ExportAllBusinessCardsJob;
import com.helger.pd.publisher.ui.AbstractAppWebPage;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.photon.bootstrap3.alert.BootstrapErrorBox;
import com.helger.photon.bootstrap3.alert.BootstrapSuccessBox;
import com.helger.photon.bootstrap3.button.BootstrapButtonToolbar;
import com.helger.photon.core.ajax.decl.AjaxFunctionDeclaration;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroDocument;

public final class PageSecureParticipantActions extends AbstractAppWebPage
{
  private static final String ACTION_UPDATE_EXPORTED_BCS = "update-exported-bcs";
  private static final Logger s_aLogger = LoggerFactory.getLogger (PageSecureParticipantActions.class);

  private static final AjaxFunctionDeclaration s_aDownloadAllIDs;
  private static final AjaxFunctionDeclaration s_aDownloadAllBCs;

  static
  {
    s_aDownloadAllIDs = addAjax ( (req, res) -> {
      final IMicroDocument aDoc = new MicroDocument ();
      final IMicroElement aRoot = aDoc.appendElement ("root");
      final ICommonsSortedSet <IParticipantIdentifier> aAllIDs = PDMetaManager.getStorageMgr ()
                                                                              .getAllContainedParticipantIDs ();
      for (final IParticipantIdentifier aParticipantID : aAllIDs)
      {
        final String sParticipantID = aParticipantID.getURIEncoded ();
        aRoot.appendElement ("item").appendText (sParticipantID);
      }
      res.xml (aDoc);
      res.attachment ("directory-participant-list.xml");
    });
    s_aDownloadAllBCs = addAjax ( (req, res) -> {
      final IMicroDocument aDoc = PDMetaManager.getStorageMgr ().getAllContainedBusinessCardsAsXML ();
      res.xml (aDoc);
      res.attachment ("directory-business-cards.xml");
    });
  }

  public PageSecureParticipantActions (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Participant actions");
  }

  @Override
  protected void fillContent (final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final IRequestWebScopeWithoutResponse aRequestScope = aWPEC.getRequestScope ();

    if (aWPEC.hasAction (ACTION_UPDATE_EXPORTED_BCS))
    {
      try
      {
        ExportAllBusinessCardsJob.exportAllBusinessCards ();
        aWPEC.postRedirectGetInternal (new BootstrapSuccessBox ().addChild ("The new exported data is now available"));
      }
      catch (final IOException ex)
      {
        s_aLogger.error ("Internal error exporting all business cards", ex);
        aWPEC.postRedirectGetInternal (new BootstrapErrorBox ().addChild ("Error exporting business cards. Technical details: " +
                                                                          ex.getMessage ()));
      }
    }

    {
      final BootstrapButtonToolbar aToolbar = getUIHandler ().createToolbar (aWPEC);
      aToolbar.addButton ("Download all IDs", s_aDownloadAllIDs.getInvocationURL (aRequestScope), EDefaultIcon.SAVE);
      aToolbar.addButton ("Download all Business Cards",
                          s_aDownloadAllBCs.getInvocationURL (aRequestScope),
                          EDefaultIcon.SAVE);
      aNodeList.addChild (aToolbar);
    }
    {
      final BootstrapButtonToolbar aToolbar = getUIHandler ().createToolbar (aWPEC);
      aToolbar.addButton ("Update all Business Cards for export",
                          aWPEC.getSelfHref ().add (CPageParam.PARAM_ACTION, ACTION_UPDATE_EXPORTED_BCS),
                          EDefaultIcon.REFRESH);
      aNodeList.addChild (aToolbar);
    }
  }
}
