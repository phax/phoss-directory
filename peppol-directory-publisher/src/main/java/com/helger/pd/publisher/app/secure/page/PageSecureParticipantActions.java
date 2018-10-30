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
import java.util.Set;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.indexer.storage.EQueryMode;
import com.helger.pd.publisher.exportall.ExportAllBusinessCardsJob;
import com.helger.pd.publisher.exportall.ExportAllManager;
import com.helger.pd.publisher.servlet.ExportDeliveryHttpHandler;
import com.helger.pd.publisher.servlet.ExportServlet;
import com.helger.pd.publisher.ui.AbstractAppWebPage;
import com.helger.pd.publisher.updater.SyncAllBusinessCardsJob;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.photon.bootstrap3.alert.BootstrapErrorBox;
import com.helger.photon.bootstrap3.alert.BootstrapSuccessBox;
import com.helger.photon.bootstrap3.button.BootstrapButtonToolbar;
import com.helger.photon.core.ajax.decl.AjaxFunctionDeclaration;
import com.helger.photon.core.url.LinkHelper;
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
  private static final String ACTION_SYNC_BCS = "sync-bcs";
  private static final Logger LOGGER = LoggerFactory.getLogger (PageSecureParticipantActions.class);

  private static final AjaxFunctionDeclaration s_aDownloadAllIDs;
  private static final AjaxFunctionDeclaration s_aDownloadAllBCs;

  static
  {
    s_aDownloadAllIDs = addAjax ( (req, res) -> {
      final IMicroDocument aDoc = new MicroDocument ();
      final IMicroElement aRoot = aDoc.appendElement ("root");
      final Set <IParticipantIdentifier> aAllIDs = PDMetaManager.getStorageMgr ()
                                                                .getAllContainedParticipantIDs (EQueryMode.NON_DELETED_ONLY)
                                                                .keySet ();
      for (final IParticipantIdentifier aParticipantID : aAllIDs)
      {
        final String sParticipantID = aParticipantID.getURIEncoded ();
        aRoot.appendElement ("item").appendText (sParticipantID);
      }
      res.xml (aDoc);
      res.attachment ("directory-participant-list.xml");
    });
    s_aDownloadAllBCs = addAjax ( (req, res) -> {
      final IMicroDocument aDoc = ExportAllManager.getAllContainedBusinessCardsAsXML (EQueryMode.NON_DELETED_ONLY);
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
        LOGGER.error ("Internal error exporting all business cards", ex);
        aWPEC.postRedirectGetInternal (new BootstrapErrorBox ().addChild ("Error exporting business cards. Technical details: " +
                                                                          ex.getMessage ()));
      }
    }
    else
      if (aWPEC.hasAction (ACTION_SYNC_BCS))
      {
        SyncAllBusinessCardsJob.syncAllBusinessCards ();
        aWPEC.postRedirectGetInternal (new BootstrapSuccessBox ().addChild ("The synchronization was started successfully and is now running in the background."));
      }

    {
      final BootstrapButtonToolbar aToolbar = getUIHandler ().createToolbar (aWPEC);
      aToolbar.addButton ("Download all IDs (uncached)",
                          s_aDownloadAllIDs.getInvocationURL (aRequestScope),
                          EDefaultIcon.SAVE);
      aToolbar.addButton ("Download all Business Cards (uncached) (may take long time)",
                          s_aDownloadAllBCs.getInvocationURL (aRequestScope),
                          EDefaultIcon.CANCEL);
      aToolbar.addButton ("Download all Business Cards (cached)",
                          LinkHelper.getURLWithContext (aRequestScope,
                                                        ExportServlet.SERVLET_DEFAULT_PATH +
                                                                       ExportDeliveryHttpHandler.SPECIAL_BUSINESS_CARDS),
                          EDefaultIcon.SAVE_ALL);
      aNodeList.addChild (aToolbar);
    }

    {
      final BootstrapButtonToolbar aToolbar = getUIHandler ().createToolbar (aWPEC);
      aToolbar.addButton ("Update all Business Cards for export",
                          aWPEC.getSelfHref ().add (CPageParam.PARAM_ACTION, ACTION_UPDATE_EXPORTED_BCS),
                          EDefaultIcon.INFO);
      aNodeList.addChild (aToolbar);
    }

    {
      final BootstrapButtonToolbar aToolbar = getUIHandler ().createToolbar (aWPEC);
      aToolbar.addButton ("Synchronize all Business Cards (re-query from SMP)",
                          aWPEC.getSelfHref ().add (CPageParam.PARAM_ACTION, ACTION_SYNC_BCS),
                          EDefaultIcon.REFRESH);
      aNodeList.addChild (aToolbar);
    }
  }
}
