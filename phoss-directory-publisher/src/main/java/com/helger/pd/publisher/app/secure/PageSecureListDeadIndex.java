/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.pd.indexer.mgr.PDIndexerManager;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.indexer.reindex.IReIndexWorkItemList;
import com.helger.photon.uicore.page.WebPageExecutionContext;

public final class PageSecureListDeadIndex extends AbstractPageSecureReIndex
{
  public PageSecureListDeadIndex (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Dead Index List", true);
  }

  @Override
  @Nonnull
  protected IReIndexWorkItemList getReIndexWorkItemList ()
  {
    final PDIndexerManager aIndexerMgr = PDMetaManager.getIndexerMgr ();
    return aIndexerMgr.getDeadList ();
  }

  @Override
  protected void showListOfExistingObjects (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    aNodeList.addChild (info ("This page contains all entries where indexing failed totally."));
    super.showListOfExistingObjects (aWPEC);
  }
}
