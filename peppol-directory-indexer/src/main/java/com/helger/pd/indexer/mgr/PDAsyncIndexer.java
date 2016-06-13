package com.helger.pd.indexer.mgr;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.concurrent.collector.IConcurrentPerformer;
import com.helger.commons.state.ESuccess;
import com.helger.pd.indexer.index.IIndexerWorkItem;
import com.helger.pd.indexer.reindex.ReIndexWorkItem;

final class PDAsyncIndexer implements IConcurrentPerformer <IIndexerWorkItem>
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (PDAsyncIndexer.class);

  private final IPDStorageManager m_aStorageMgr;
  private final PDIndexerManager m_aIndexerMgr;

  public PDAsyncIndexer (@Nonnull final IPDStorageManager aStorageMgr, @Nonnull final PDIndexerManager aIndexerMgr)
  {
    m_aStorageMgr = aStorageMgr;
    m_aIndexerMgr = aIndexerMgr;
  }

  public void runAsync (@Nonnull final IIndexerWorkItem aItem) throws Exception
  {
    final ESuccess eSuccess = PDIndexExecutor.executeWorkItem (m_aStorageMgr, m_aIndexerMgr, aItem);

    if (eSuccess.isFailure ())
    {
      s_aLogger.warn ("Error fetching " + aItem.getLogText ());
      // Failed to fetch participant data - add to re-index queue and leave in
      // the overall list
      m_aIndexerMgr.internalGetMutableReIndexList ().addItem (new ReIndexWorkItem (aItem));
    }
  }
}
