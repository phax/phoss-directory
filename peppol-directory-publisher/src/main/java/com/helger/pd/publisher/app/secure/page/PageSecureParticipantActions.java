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

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.ICommonsSortedSet;
import com.helger.commons.url.ISimpleURL;
import com.helger.html.hc.html.sections.HCH3;
import com.helger.html.hc.html.tabular.HCCol;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.html.tabular.HCTable;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.publisher.app.pub.CMenuPublic;
import com.helger.pd.publisher.app.pub.page.PagePublicSearchSimple;
import com.helger.pd.publisher.app.secure.CMenuSecure;
import com.helger.pd.publisher.ui.AbstractAppWebPage;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.photon.basic.app.appid.CApplicationID;
import com.helger.photon.bootstrap3.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.page.WebPageExecutionContext;

public final class PageSecureParticipantActions extends AbstractAppWebPage
{
  public PageSecureParticipantActions (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Participant actions");
  }

  @Override
  protected void fillContent (final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();

    final ICommonsSortedSet <IParticipantIdentifier> aAllIDs = PDMetaManager.getStorageMgr ()
                                                                            .getAllContainedParticipantIDs ();
    aNodeList.addChild (new HCH3 ().addChild (aAllIDs.size () + " participants are contained"));

    final HCTable aTable = new HCTable (HCCol.star (), HCCol.star (), HCCol.star ()).setID (getID ());
    aTable.addHeaderRow ().addCells ("ID", "Actions");
    for (final IParticipantIdentifier aParticipantID : aAllIDs)
    {
      final String sParticipantID = aParticipantID.getURIEncoded ();

      final HCRow aRow = aTable.addBodyRow ();
      aRow.addCell (sParticipantID);

      final ISimpleURL aShowDetails = aWPEC.getLinkToMenuItem (CApplicationID.APP_ID_PUBLIC,
                                                               CMenuPublic.MENU_SEARCH_SIMPLE)
                                           .add (PagePublicSearchSimple.FIELD_QUERY, sParticipantID)
                                           .add (CPageParam.PARAM_ACTION, CPageParam.ACTION_VIEW)
                                           .add (PagePublicSearchSimple.FIELD_PARTICIPANT_ID, sParticipantID);
      aRow.addCell (new HCA (aShowDetails).addChild ("Search"));

      final ISimpleURL aReIndex = aWPEC.getLinkToMenuItem (CMenuSecure.MENU_INDEX_MANUALLY)
                                       .add (PageSecureIndexManually.FIELD_PARTICIPANT_ID, sParticipantID)
                                       .add (CPageParam.PARAM_ACTION, CPageParam.ACTION_PERFORM);
      aRow.addCell (new HCA (aReIndex).addChild ("Reindex"));
    }

    aNodeList.addChild (aTable).addChild (BootstrapDataTables.createDefaultDataTables (aWPEC, aTable));
  }
}
