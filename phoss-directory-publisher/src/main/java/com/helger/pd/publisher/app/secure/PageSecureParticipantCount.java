/**
 * Copyright (C) 2015-2021 Philip Helger (www.helger.com)
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

import javax.annotation.Nonnull;

import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.MatchAllDocsQuery;

import com.helger.commons.annotation.Nonempty;
import com.helger.html.hc.html.grouping.HCHR;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.pd.indexer.lucene.AllDocumentsCollector;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.indexer.storage.EQueryMode;
import com.helger.pd.publisher.ui.AbstractAppWebPage;
import com.helger.photon.bootstrap4.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.bootstrap4.table.BootstrapTable;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.WebPageExecutionContext;

public final class PageSecureParticipantCount extends AbstractAppWebPage
{
  public PageSecureParticipantCount (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Participant count");
  }

  @Override
  protected void fillContent (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();

    {
      final BootstrapButtonToolbar aToolbar = new BootstrapButtonToolbar (aWPEC);
      aToolbar.addButton ("Refresh", aWPEC.getSelfHref (), EDefaultIcon.MAGNIFIER);
      aNodeList.addChild (aToolbar);
    }

    final int nNotDeletedCount = PDMetaManager.getStorageMgr ().getContainedParticipantCount (EQueryMode.NON_DELETED_ONLY);
    aNodeList.addChild (h3 (nNotDeletedCount + " participants (entities) are contained"));

    final int nDeletedCount = PDMetaManager.getStorageMgr ().getContainedParticipantCount (EQueryMode.DELETED_ONLY);
    aNodeList.addChild (h3 (nDeletedCount + " deleted participants (entities) are contained"));

    final int nReIndexCount = PDMetaManager.getIndexerMgr ().getReIndexList ().getItemCount ();
    aNodeList.addChild (h3 (nReIndexCount + " re-index items are contained"));

    final int nDeadCount = PDMetaManager.getIndexerMgr ().getDeadList ().getItemCount ();
    aNodeList.addChild (h3 (nDeadCount + " dead items are contained"));

    if (false)
      try
      {
        final Collector aCollector = new AllDocumentsCollector (PDMetaManager.getLucene (), (aDoc, nIdx) -> {
          final BootstrapTable aTable = new BootstrapTable ();
          for (final IndexableField f : aDoc.getFields ())
            aTable.addBodyRow ().addCells (f.name (), f.fieldType ().toString (), f.stringValue ());
          aNodeList.addChild (aTable);
          aNodeList.addChild (new HCHR ());
        });
        PDMetaManager.getStorageMgr ().searchAtomic (new MatchAllDocsQuery (), aCollector);
      }
      catch (final IOException ex)
      {}
  }
}
