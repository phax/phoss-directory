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
package com.helger.pd.publisher.app.secure.page;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.ICommonsSortedSet;
import com.helger.commons.url.ISimpleURL;
import com.helger.html.hc.html.sections.HCH3;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.html.tabular.IHCCell;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.publisher.app.pub.CMenuPublic;
import com.helger.pd.publisher.app.pub.page.PagePublicSearchSimple;
import com.helger.pd.publisher.ui.AbstractAppWebPage;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.photon.basic.app.CApplicationID;
import com.helger.photon.bootstrap3.alert.BootstrapInfoBox;
import com.helger.photon.bootstrap3.button.BootstrapButtonToolbar;
import com.helger.photon.bootstrap3.table.BootstrapTable;
import com.helger.photon.core.ajax.decl.AjaxFunctionDeclaration;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroDocument;

public final class PageSecureAllParticipants extends AbstractAppWebPage
{
  private final AjaxFunctionDeclaration m_aExportAll;

  public PageSecureAllParticipants (@Nonnull @Nonempty final String sID)
  {
    super (sID, "All participants");
    m_aExportAll = addAjax ( (req, res) -> {
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
    });
  }

  @Override
  protected void fillContent (final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final IRequestWebScopeWithoutResponse aRequestScope = aWPEC.getRequestScope ();

    {
      final BootstrapButtonToolbar aToolbar = getUIHandler ().createToolbar (aWPEC);
      aToolbar.addButton ("Export", m_aExportAll.getInvocationURL (aRequestScope), EDefaultIcon.SAVE);
      aNodeList.addChild (aToolbar);
    }

    final ICommonsSortedSet <IParticipantIdentifier> aAllIDs = PDMetaManager.getStorageMgr ()
                                                                            .getAllContainedParticipantIDs ();
    aNodeList.addChild (new HCH3 ().addChild (aAllIDs.size () + " participants are contained"));

    final BootstrapTable aTable = new BootstrapTable ().setCondensed (true).setBordered (true);
    for (final IParticipantIdentifier aParticipantID : aAllIDs)
    {
      final String sParticipantID = aParticipantID.getURIEncoded ();

      final HCRow aRow = aTable.addBodyRow ();
      aRow.addCell (sParticipantID);

      final IHCCell <?> aActionCell = aRow.addCell ();

      final ISimpleURL aShowDetails = aWPEC.getLinkToMenuItem (CApplicationID.APP_ID_PUBLIC,
                                                               CMenuPublic.MENU_SEARCH_SIMPLE)
                                           .add (PagePublicSearchSimple.FIELD_QUERY, sParticipantID)
                                           .add (CPageParam.PARAM_ACTION, CPageParam.ACTION_VIEW)
                                           .add (PagePublicSearchSimple.FIELD_PARTICIPANT_ID, sParticipantID);
      aActionCell.addChild (new HCA (aShowDetails).addChild ("Search"));
    }

    if (aTable.hasBodyRows ())
      aNodeList.addChild (aTable);
    else
      aNodeList.addChild (new BootstrapInfoBox ().addChild ("No participant identifier is yet in the index"));
  }
}
