/*
 * Copyright (C) 2015-2023 Philip Helger (www.helger.com)
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
package com.helger.pd.indexer.index;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.concurrent.BasicThreadFactory;
import com.helger.commons.concurrent.ExecutorServiceHelper;
import com.helger.commons.concurrent.collector.ConcurrentCollectorSingle;
import com.helger.commons.concurrent.collector.IConcurrentPerformer;

/**
 * The indexer queue that holds all items to be indexed initially. If indexing
 * fails, items are shifted to the re-index list (see
 * {@link com.helger.pd.indexer.reindex.ReIndexWorkItemList}) where graceful
 * retries will happen.
 *
 * @author Philip Helger
 */
public final class IndexerWorkItemQueue
{
  private final LinkedBlockingQueue <Object> m_aQueue;
  private final ConcurrentCollectorSingle <IIndexerWorkItem> m_aImmediateCollector;
  private final ThreadFactory m_aThreadFactory = new BasicThreadFactory.Builder ().namingPattern ("pd-indexer-%d")
                                                                                  .daemon (false)
                                                                                  .priority (Thread.NORM_PRIORITY)
                                                                                  .build ();

  private final ExecutorService m_aSenderThreadPool = new ThreadPoolExecutor (1,
                                                                              2,
                                                                              60L,
                                                                              TimeUnit.SECONDS,
                                                                              new SynchronousQueue <Runnable> (),
                                                                              m_aThreadFactory);

  /**
   * Constructor.
   *
   * @param aPerformer
   *        The executor that will effective handle work items (e.g. retrieve
   *        from SMP).
   */
  public IndexerWorkItemQueue (@Nonnull final IConcurrentPerformer <IIndexerWorkItem> aPerformer)
  {
    ValueEnforcer.notNull (aPerformer, "Performer");
    // Use an indefinite queue for holding tasks
    m_aQueue = new LinkedBlockingQueue <> ();
    m_aImmediateCollector = new ConcurrentCollectorSingle <> (m_aQueue);
    m_aImmediateCollector.setPerformer (aPerformer);

    // Start the collector
    m_aSenderThreadPool.submit (m_aImmediateCollector::collect);
  }

  /**
   * Stop the indexer work queue immediately.
   *
   * @return The list of all remaining objects in the queue. Never
   *         <code>null</code>.
   */
  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IIndexerWorkItem> stop ()
  {
    // don't take any more actions
    m_aImmediateCollector.stopQueuingNewObjects ();

    // Get all remaining objects and save them for late reuse
    final ICommonsList <IIndexerWorkItem> aRemainingItems = m_aImmediateCollector.drainQueue ();

    // Shutdown the thread pool afterwards
    ExecutorServiceHelper.shutdownAndWaitUntilAllTasksAreFinished (m_aSenderThreadPool);

    return aRemainingItems;
  }

  /**
   * @return The internal queue. Handle with care - usually you don't need that
   *         one. Never <code>null</code>,
   */
  @Nonnull
  @ReturnsMutableObject
  public LinkedBlockingQueue <Object> internalGetQueue ()
  {
    return m_aQueue;
  }

  /**
   * Queue a work item and handle it asynchronously.
   *
   * @param aItem
   *        The item to be added. May not be <code>null</code>.
   */
  public void queueObject (@Nonnull final IIndexerWorkItem aItem)
  {
    ValueEnforcer.notNull (aItem, "Item");
    m_aImmediateCollector.queueObject (aItem);
  }
}
