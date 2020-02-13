/**
 * Copyright (C) 2015-2020 Philip Helger (www.helger.com)
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
package com.helger.pd.publisher.app.secure;

import java.util.Locale;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.compare.ESortOrder;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.datetime.PDTToString;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.pd.indexer.index.IIndexerWorkItem;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.publisher.ui.AbstractAppWebPage;
import com.helger.pd.publisher.ui.PDCommonUI;
import com.helger.photon.bootstrap4.button.BootstrapButton;
import com.helger.photon.bootstrap4.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.bootstrap4.table.BootstrapTable;
import com.helger.photon.bootstrap4.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uictrls.datatables.column.DTCol;
import com.helger.photon.uictrls.datatables.column.EDTColType;

public final class PageSecureListIndex extends AbstractAppWebPage
{
  public PageSecureListIndex (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Index Queue");
  }

  @Override
  protected void fillContent (final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();

    // Add toolbar
    {
      final BootstrapButtonToolbar aToolbar = aNodeList.addAndReturnChild (new BootstrapButtonToolbar (aWPEC));
      aToolbar.addChild (new BootstrapButton ().addChild ("Refresh")
                                               .setIcon (EDefaultIcon.REFRESH)
                                               .setOnClick (aWPEC.getSelfHref ()));
      aToolbar.addChild (span ("Current server time: " +
                               PDTToString.getAsString (PDTFactory.getCurrentLocalTime (), aDisplayLocale))
                                                                                                           .addClass (PDCommonUI.CSS_CLASS_VERTICAL_PADDED_TEXT));
    }

    final LinkedBlockingQueue <Object> aQueue = PDMetaManager.getIndexerMgr ().getIndexerWorkQueue ().getQueue ();
    final int nLength = aQueue.size ();
    if (nLength == 0)
    {
      aNodeList.addChild (success ("The Index Queue is currently empty"));
    }
    else
    {
      aNodeList.addChild (info ("The Index Queue contains " + nLength + " entries"));

      final BootstrapTable aTable = new BootstrapTable (new DTCol ("Queue date time").setDisplayType (EDTColType.DATETIME,
                                                                                                      aDisplayLocale)
                                                                                     .setInitialSorting (ESortOrder.DESCENDING),
                                                        new DTCol ("Participant ID"),
                                                        new DTCol ("Action"),
                                                        new DTCol ("Owner"),
                                                        new DTCol ("Requestor")).setID ("indexqueue");
      for (final Object o : aQueue)
        if (o instanceof IIndexerWorkItem)
        {
          final IIndexerWorkItem aObj = (IIndexerWorkItem) o;
          final HCRow aRow = aTable.addBodyRow ();
          aRow.addCell (PDTToString.getAsString (aObj.getCreationDateTime (), aDisplayLocale));
          aRow.addCell (aObj.getParticipantID ().getURIEncoded ());
          aRow.addCell (aObj.getType ().getDisplayName ());
          aRow.addCell (aObj.getOwnerID ());
          aRow.addCell (aObj.getRequestingHost ());
        }
      aNodeList.addChild (aTable).addChild (BootstrapDataTables.createDefaultDataTables (aWPEC, aTable));
    }
  }
}
