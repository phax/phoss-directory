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
package com.helger.pd.publisher.app.secure.page;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.datetime.PDTToString;
import com.helger.commons.io.stream.NonBlockingByteArrayOutputStream;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.indexer.storage.EQueryMode;
import com.helger.pd.publisher.CPDPublisher;
import com.helger.pd.publisher.exportall.ExportAllBusinessCardsJob;
import com.helger.pd.publisher.exportall.ExportAllManager;
import com.helger.pd.publisher.servlet.ExportDeliveryHttpHandler;
import com.helger.pd.publisher.servlet.ExportServlet;
import com.helger.pd.publisher.ui.AbstractAppWebPage;
import com.helger.pd.publisher.updater.SyncAllBusinessCardsJob;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.photon.ajax.decl.AjaxFunctionDeclaration;
import com.helger.photon.app.url.LinkHelper;
import com.helger.photon.bootstrap4.alert.BootstrapErrorBox;
import com.helger.photon.bootstrap4.alert.BootstrapSuccessBox;
import com.helger.photon.bootstrap4.alert.BootstrapWarnBox;
import com.helger.photon.bootstrap4.button.BootstrapButton;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.poi.excel.EExcelVersion;
import com.helger.poi.excel.WorkbookCreationHelper;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroDocument;

public final class PageSecureParticipantActions extends AbstractAppWebPage
{
  private static final String ACTION_UPDATE_EXPORTED_BCS = "update-exported-bcs";
  private static final String ACTION_SYNC_BCS_UNFORCED = "sync-bcs-unforced";
  private static final String ACTION_SYNC_BCS_FORCED = "sync-bcs-forced";
  private static final Logger LOGGER = LoggerFactory.getLogger (PageSecureParticipantActions.class);

  private static final AjaxFunctionDeclaration s_aDownloadAllIDsXML;
  private static final AjaxFunctionDeclaration s_aDownloadAllBCsXML;
  private static final AjaxFunctionDeclaration s_aDownloadAllBCsExcel;

  static
  {
    s_aDownloadAllIDsXML = addAjax ( (req, res) -> {
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
    s_aDownloadAllBCsXML = addAjax ( (req, res) -> {
      final IMicroDocument aDoc = ExportAllManager.getAllContainedBusinessCardsAsXML (EQueryMode.NON_DELETED_ONLY);
      res.xml (aDoc);
      res.attachment ("directory-business-cards.xml");
    });
    s_aDownloadAllBCsExcel = addAjax ( (req, res) -> {
      final WorkbookCreationHelper aWBCH = ExportAllManager.getAllContainedBusinessCardsAsExcel (EQueryMode.NON_DELETED_ONLY);
      try (NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ())
      {
        aWBCH.writeTo (aBAOS);
        res.binary (aBAOS, EExcelVersion.XLSX.getMimeType (), "directory-business-cards.xlsx");
      }
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
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
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
      if (aWPEC.hasAction (ACTION_SYNC_BCS_UNFORCED))
      {
        if (SyncAllBusinessCardsJob.syncAllBusinessCards (false).isChanged ())
          aWPEC.postRedirectGetInternal (new BootstrapSuccessBox ().addChild ("The unforced synchronization was started successfully and is now running in the background."));
        else
          aWPEC.postRedirectGetInternal (new BootstrapWarnBox ().addChild ("The synchronization was not started because the last sync was at " +
                                                                           PDTToString.getAsString (SyncAllBusinessCardsJob.getLastSync (),
                                                                                                    aDisplayLocale)));
      }
      else
        if (aWPEC.hasAction (ACTION_SYNC_BCS_FORCED))
        {
          if (SyncAllBusinessCardsJob.syncAllBusinessCards (true).isChanged ())
            aWPEC.postRedirectGetInternal (new BootstrapSuccessBox ().addChild ("The forced synchronization was started successfully and is now running in the background."));
          else
            aWPEC.postRedirectGetInternal (new BootstrapErrorBox ().addChild ("Force synchronization should always work"));
        }

    aNodeList.addChild (new HCDiv ().addChild (new BootstrapButton ().addChild ("Download all IDs (XML, uncached)")
                                                                     .setOnClick (s_aDownloadAllIDsXML.getInvocationURL (aRequestScope))
                                                                     .setIcon (EDefaultIcon.SAVE)));
    aNodeList.addChild (new HCDiv ().addChild (new BootstrapButton ().addChild ("Download all Business Cards (XML, uncached) (may take long time)")
                                                                     .setOnClick (s_aDownloadAllBCsXML.getInvocationURL (aRequestScope))
                                                                     .setIcon (EDefaultIcon.CANCEL)));
    aNodeList.addChild (new HCDiv ().addChild (new BootstrapButton ().addChild ("Download all Business Cards (Excel, uncached) (may take long time)")
                                                                     .setOnClick (s_aDownloadAllBCsExcel.getInvocationURL (aRequestScope))
                                                                     .setIcon (EDefaultIcon.CANCEL)));
    aNodeList.addChild (new HCDiv ().addChild (new BootstrapButton ().addChild ("Download all Business Cards (XML, cached)")
                                                                     .setOnClick (LinkHelper.getURLWithContext (aRequestScope,
                                                                                                                ExportServlet.SERVLET_DEFAULT_PATH +
                                                                                                                               ExportDeliveryHttpHandler.SPECIAL_BUSINESS_CARDS_XML))
                                                                     .setIcon (EDefaultIcon.SAVE_ALL)));
    if (CPDPublisher.EXCEL_EXPORT)
    {
      aNodeList.addChild (new HCDiv ().addChild (new BootstrapButton ().addChild ("Download all Business Cards (Excel, cached)")
                                                                       .setOnClick (LinkHelper.getURLWithContext (aRequestScope,
                                                                                                                  ExportServlet.SERVLET_DEFAULT_PATH +
                                                                                                                                 ExportDeliveryHttpHandler.SPECIAL_BUSINESS_CARDS_EXCEL))
                                                                       .setIcon (EDefaultIcon.SAVE_ALL)));
    }
    aNodeList.addChild (new HCDiv ().addChild (new BootstrapButton ().addChild ("Update all Business Cards for export (XML" +
                                                                                (CPDPublisher.EXCEL_EXPORT ? " and Excel"
                                                                                                           : "") +
                                                                                ")")
                                                                     .setOnClick (aWPEC.getSelfHref ()
                                                                                       .add (CPageParam.PARAM_ACTION,
                                                                                             ACTION_UPDATE_EXPORTED_BCS))
                                                                     .setIcon (EDefaultIcon.INFO)));
    aNodeList.addChild (new HCDiv ().addChild (new BootstrapButton ().addChild ("Synchronize all Business Cards (re-query from SMP - unforced)")
                                                                     .setOnClick (aWPEC.getSelfHref ()
                                                                                       .add (CPageParam.PARAM_ACTION,
                                                                                             ACTION_SYNC_BCS_UNFORCED))
                                                                     .setIcon (EDefaultIcon.REFRESH)));
    aNodeList.addChild (new HCDiv ().addChild (new BootstrapButton ().addChild ("Synchronize all Business Cards (re-query from SMP - forced)")
                                                                     .setOnClick (aWPEC.getSelfHref ()
                                                                                       .add (CPageParam.PARAM_ACTION,
                                                                                             ACTION_SYNC_BCS_FORCED))
                                                                     .setIcon (EDefaultIcon.CANCEL)));
  }
}
