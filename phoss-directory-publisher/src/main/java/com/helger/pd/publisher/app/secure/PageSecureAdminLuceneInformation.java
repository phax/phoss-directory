/*
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

import org.apache.lucene.index.DirectoryReader;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.lang.StackTraceHelper;
import com.helger.html.hc.ext.HCExtHelper;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.pd.indexer.lucene.PDLucene;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.publisher.ui.AbstractAppWebPage;
import com.helger.photon.bootstrap4.table.BootstrapTable;
import com.helger.photon.uicore.page.WebPageExecutionContext;

/**
 * Information on the Lucene Index.
 *
 * @author Philip Helger
 * @since 0.7.1
 */
public final class PageSecureAdminLuceneInformation extends AbstractAppWebPage
{
  public PageSecureAdminLuceneInformation (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Lucene information");
  }

  @Override
  protected void fillContent (final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final PDLucene aLucene = PDMetaManager.getLucene ();

    final BootstrapTable aTable = new BootstrapTable ();
    aTable.addBodyRow ().addCells ("Lucene index directory", PDLucene.getLuceneIndexDir ().getAbsolutePath ());
    try
    {
      final DirectoryReader aReader = aLucene.getReader ();
      if (aReader != null)
        aTable.addBodyRow ().addCells ("Directory information", aReader.toString ());
    }
    catch (final IOException ex)
    {
      aTable.addBodyRow ()
            .addCell ("Directory information")
            .addCell (HCExtHelper.nl2divList (ex.getClass ().getName () + "\n" + StackTraceHelper.getStackAsString (ex)));
    }
    aNodeList.addChild (aTable);
  }
}
