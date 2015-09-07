/**
 * Copyright (C) 2015 Philip Helger (www.helger.com)
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
package com.helger.pyp.indexer;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.callback.IThrowingRunnableWithParameter;
import com.helger.commons.concurrent.ExtendedDefaultThreadFactory;
import com.helger.commons.concurrent.ManagedExecutorService;
import com.helger.commons.concurrent.collector.ConcurrentCollectorSingle;

/**
 * The indexer queue that holds all items to be indexed initially. If indexing
 * fails, items are shifted to the re-index queue where graceful retries will
 * happen.
 *
 * @author Philip Helger
 */
final class IndexerWorkItemQueue
{
  private final ConcurrentCollectorSingle <IndexerWorkItem> m_aImmediateCollector;
  private final ThreadFactory m_aThreadFactory = new ExtendedDefaultThreadFactory ("IndexerWorkQueue");
  private final ExecutorService m_aSenderThreadPool = new ThreadPoolExecutor (1,
                                                                              1,
                                                                              60L,
                                                                              TimeUnit.SECONDS,
                                                                              new SynchronousQueue <Runnable> (),
                                                                              m_aThreadFactory);

  public IndexerWorkItemQueue (@Nonnull final IThrowingRunnableWithParameter <IndexerWorkItem> aPerformer)
  {
    m_aImmediateCollector = new ConcurrentCollectorSingle <> (new LinkedBlockingQueue <> ());
    m_aImmediateCollector.setPerformer (aPerformer);

    // Start the collector
    m_aSenderThreadPool.submit (m_aImmediateCollector);
  }

  /**
   * Stop the indexer work queue immediately.
   *
   * @return The list of all remaining objects in the queue. Never
   *         <code>null</code>.
   */
  @Nonnull
  @ReturnsMutableCopy
  public List <IndexerWorkItem> stop ()
  {
    // don't take any more actions
    m_aSenderThreadPool.shutdown ();
    m_aImmediateCollector.stopQueuingNewObjects ();

    // Get all remaining objects and save them for late reuse
    final List <IndexerWorkItem> aRemainingItems = m_aImmediateCollector.drainQueue ();

    // Shutdown the thread pool
    ManagedExecutorService.shutdownAndWaitUntilAllTasksAreFinished (m_aSenderThreadPool);

    return aRemainingItems;
  }

  public void queueObject (@Nonnull final IndexerWorkItem aItem)
  {
    ValueEnforcer.notNull (aItem, "Item");
    m_aImmediateCollector.queueObject (aItem);
  }
}
