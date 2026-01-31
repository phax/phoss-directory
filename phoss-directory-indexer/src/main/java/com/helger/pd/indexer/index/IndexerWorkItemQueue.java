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
package com.helger.pd.indexer.index;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.base.concurrent.BasicThreadFactoryBuilder;
import com.helger.base.concurrent.ExecutorServiceHelper;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.reflection.GenericReflection;
import com.helger.base.state.ESuccess;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.commons.concurrent.collector.ConcurrentCollectorSingle;
import com.helger.commons.concurrent.collector.IConcurrentPerformer;

/**
 * The indexer queue that holds all items to be indexed initially. If indexing fails, items are
 * shifted to the re-index list (see {@link com.helger.pd.indexer.reindex.ReIndexWorkItemList})
 * where graceful retries will happen.
 *
 * @author Philip Helger
 */
public final class IndexerWorkItemQueue
{
  private static final int MAX_PARALLEL = 4;
  private final LinkedBlockingQueue <Object> m_aQueue;
  private final ThreadFactory m_aThreadFactory = new BasicThreadFactoryBuilder ().namingPattern ("pd-indexer-%d")
                                                                                 .daemon (false)
                                                                                 .priority (Thread.NORM_PRIORITY)
                                                                                 .build ();
  private final ExecutorService m_aSenderThreadPool = new ThreadPoolExecutor (MAX_PARALLEL,
                                                                              MAX_PARALLEL,
                                                                              60L,
                                                                              TimeUnit.SECONDS,
                                                                              new SynchronousQueue <> (),
                                                                              m_aThreadFactory);
  private final ConcurrentCollectorSingle <IIndexerWorkItem> [] m_aImmediateCollector = GenericReflection.uncheckedCast (new ConcurrentCollectorSingle [MAX_PARALLEL]);

  /**
   * Constructor.
   *
   * @param aPerformer
   *        The executor that will effective handle work items (e.g. retrieve from SMP).
   */
  public IndexerWorkItemQueue (@NonNull final IConcurrentPerformer <IIndexerWorkItem> aPerformer)
  {
    ValueEnforcer.notNull (aPerformer, "Performer");
    // Use an indefinite queue for holding tasks
    // It's a thread-safe collection
    m_aQueue = new LinkedBlockingQueue <> ();

    // Start the collector(s)
    for (int i = 0; i < MAX_PARALLEL; ++i)
    {
      m_aImmediateCollector[i] = new ConcurrentCollectorSingle <> (m_aQueue);
      m_aImmediateCollector[i].setPerformer (aPerformer);
      m_aSenderThreadPool.submit (m_aImmediateCollector[i]::collect);
    }
  }

  /**
   * Stop the indexer work queue immediately.
   *
   * @return The list of all remaining objects in the queue. Never <code>null</code>.
   */
  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <IIndexerWorkItem> stop ()
  {
    // don't take any more actions
    for (int i = 0; i < MAX_PARALLEL; ++i)
      m_aImmediateCollector[i].stopQueuingNewObjects ();

    // Get all remaining objects and save them for later reuse
    final ICommonsList <IIndexerWorkItem> aRemainingItems = new CommonsArrayList <> ();
    for (int i = 0; i < MAX_PARALLEL; ++i)
      aRemainingItems.addAll (m_aImmediateCollector[i].drainQueue ());

    // Shutdown the thread pool afterwards
    ExecutorServiceHelper.shutdownAndWaitUntilAllTasksAreFinished (m_aSenderThreadPool);

    return aRemainingItems;
  }

  /**
   * @return The internal queue. Handle with care - usually you don't need that one. Never
   *         <code>null</code>,
   */
  @NonNull
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
   * @return {@link ESuccess#SUCCESS} if queuing worked, {@link ESuccess#FAILURE} otherwise.
   */
  @NonNull
  public ESuccess queueObject (@NonNull final IIndexerWorkItem aItem)
  {
    ValueEnforcer.notNull (aItem, "Item");
    // Always queue at the first - as they all work on the same queue
    return m_aImmediateCollector[0].queueObject (aItem);
  }

  /**
   * @return The amount of elements currently in the queue.
   */
  @Nonnegative
  public int getQueueLength ()
  {
    return m_aQueue.size ();
  }
}
