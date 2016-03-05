/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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

import java.util.Locale;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.compare.ESortOrder;
import com.helger.commons.type.EBaseType;
import com.helger.datetime.format.PDTToString;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.html.tabular.HCTable;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.pd.indexer.domain.IndexerWorkItem;
import com.helger.pd.indexer.domain.ReIndexWorkItem;
import com.helger.pd.indexer.mgr.IReIndexWorkItemList;
import com.helger.pd.publisher.ui.AbstractAppWebPage;
import com.helger.photon.bootstrap3.button.BootstrapButton;
import com.helger.photon.bootstrap3.button.BootstrapButtonToolbar;
import com.helger.photon.bootstrap3.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uictrls.datatables.column.DTCol;

public abstract class AbstractPageSecureReIndex extends AbstractAppWebPage
{
  public AbstractPageSecureReIndex (@Nonnull @Nonempty final String sID, @Nonnull final String sName)
  {
    super (sID, sName);
  }

  @Nonnull
  protected abstract IReIndexWorkItemList getReIndexWorkItemList ();

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
    }

    final HCTable aTable = new HCTable (new DTCol ("Reg date").setDisplayType (EBaseType.DATETIME, aDisplayLocale)
                                                              .setInitialSorting (ESortOrder.DESCENDING),
                                        new DTCol ("Participant"),
                                        new DTCol ("Type"),
                                        new DTCol ("Retries").setDisplayType (EBaseType.INT, aDisplayLocale),
                                        new DTCol ("Next retry").setDisplayType (EBaseType.DATETIME, aDisplayLocale),
                                        new DTCol ("Last retry").setDisplayType (EBaseType.DATETIME,
                                                                                 aDisplayLocale)).setID (getID ());

    for (final ReIndexWorkItem aItem : getReIndexWorkItemList ().getAllItems ())
    {
      final IndexerWorkItem aWorkItem = aItem.getWorkItem ();

      final HCRow aRow = aTable.addBodyRow ();
      aRow.addCell (PDTToString.getAsString (aWorkItem.getCreationDT (), aDisplayLocale));
      aRow.addCell (aWorkItem.getParticipantID ().getURIEncoded ());
      aRow.addCell (aWorkItem.getType ().getDisplayName ());
      aRow.addCell (Integer.toString (aItem.getRetryCount ()));
      aRow.addCell (PDTToString.getAsString (aItem.getNextRetryDT (), aDisplayLocale));
      aRow.addCell (PDTToString.getAsString (aItem.getMaxRetryDT (), aDisplayLocale));
    }

    aNodeList.addChild (aTable);
    aNodeList.addChild (BootstrapDataTables.createDefaultDataTables (aWPEC, aTable));
  }
}
