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

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;

import com.helger.commons.annotation.Nonempty;
import com.helger.html.hc.html.grouping.HCHR;
import com.helger.html.hc.html.sections.HCH3;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.publisher.ui.AbstractAppWebPage;
import com.helger.photon.bootstrap3.table.BootstrapTable;
import com.helger.photon.uicore.page.WebPageExecutionContext;

public final class PageSecureParticipantCount extends AbstractAppWebPage
{
  public PageSecureParticipantCount (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Participant count");
  }

  @Override
  protected void fillContent (final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();

    final int nNotDeletedCount = PDMetaManager.getStorageMgr ().getContainedNotDeletedParticipantCount ();
    aNodeList.addChild (new HCH3 ().addChild (nNotDeletedCount + " participants (entities) are contained"));

    final int nDeletedCount = PDMetaManager.getStorageMgr ().getContainedDeletedParticipantCount ();
    aNodeList.addChild (new HCH3 ().addChild (nDeletedCount + " deleted participants (entities) are contained"));

    final int nReIndexCount = PDMetaManager.getIndexerMgr ().getReIndexList ().getItemCount ();
    aNodeList.addChild (new HCH3 ().addChild (nReIndexCount + " re-index items are contained"));

    final int nDeadCount = PDMetaManager.getIndexerMgr ().getDeadList ().getItemCount ();
    aNodeList.addChild (new HCH3 ().addChild (nDeadCount + " dead items are contained"));

    if (false)
      try
      {
        final TopScoreDocCollector aCollector = TopScoreDocCollector.create (5);
        PDMetaManager.getStorageMgr ().searchAtomic (new MatchAllDocsQuery (), aCollector);
        for (final ScoreDoc aScoreDoc : aCollector.topDocs ().scoreDocs)
        {
          final Document aDoc = PDMetaManager.getLucene ().getDocument (aScoreDoc.doc);
          if (aDoc == null)
            throw new IllegalStateException ("Failed to resolve Lucene Document with ID " + aScoreDoc.doc);
          // Pass to Consumer

          final BootstrapTable aTable = new BootstrapTable ();
          for (final IndexableField f : aDoc.getFields ())
            aTable.addBodyRow ().addCells (f.name (), f.fieldType ().toString (), f.stringValue ());
          aNodeList.addChild (aTable);
          aNodeList.addChild (new HCHR ());
        }
      }
      catch (final IOException ex)
      {}
  }
}
