/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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

import java.io.IOException;
import java.util.Map;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.base.rt.StackTraceHelper;
import com.helger.html.hc.ext.HCExtHelper;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.publisher.ui.AbstractAppWebPage;
import com.helger.photon.bootstrap5.table.BootstrapTable;
import com.helger.photon.uicore.page.WebPageExecutionContext;

/**
 * Information on the search index. The content depends on the search index implementation in use.
 *
 * @author Philip Helger
 * @since 0.7.1
 */
public final class PageSecureAdminLuceneInformation extends AbstractAppWebPage
{
  public PageSecureAdminLuceneInformation (@NonNull @Nonempty final String sID)
  {
    super (sID, "Search index information");
  }

  @Override
  protected void fillContent (final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();

    final BootstrapTable aTable = new BootstrapTable ();
    try
    {
      for (final Map.Entry <String, String> aEntry : PDMetaManager.getIndex ().getIndexInformation ().entrySet ())
        aTable.addBodyRow ().addCells (aEntry.getKey (), aEntry.getValue ());
    }
    catch (final IOException ex)
    {
      aTable.addBodyRow ()
            .addCell ("Index information")
            .addCell (HCExtHelper.nl2divList (ex.getClass ().getName () +
                                              "\n" +
                                              StackTraceHelper.getStackAsString (ex)));
    }
    aNodeList.addChild (aTable);
  }
}
