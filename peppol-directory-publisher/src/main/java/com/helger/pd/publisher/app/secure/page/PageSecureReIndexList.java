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

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;
import com.helger.pd.indexer.mgr.PDIndexerManager;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.indexer.reindex.IReIndexWorkItemList;

public final class PageSecureReIndexList extends AbstractPageSecureReIndex
{
  public PageSecureReIndexList (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Re-Index List");
  }

  @Override
  @Nonnull
  protected IReIndexWorkItemList getReIndexWorkItemList ()
  {
    final PDIndexerManager aIndexerMgr = PDMetaManager.getIndexerMgr ();
    return aIndexerMgr.getReIndexList ();
  }
}
