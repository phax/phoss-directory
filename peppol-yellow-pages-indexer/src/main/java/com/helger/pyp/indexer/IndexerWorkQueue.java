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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.concurrent.ExtendedDefaultThreadFactory;
import com.helger.commons.concurrent.ManagedExecutorService;
import com.helger.commons.concurrent.collector.ConcurrentCollectorSingle;
import com.helger.commons.scope.IScope;
import com.helger.commons.scope.singleton.AbstractGlobalSingleton;
import com.helger.peppol.identifier.IParticipantIdentifier;

public class IndexerWorkQueue extends AbstractGlobalSingleton
{
  private static final class IndexerCollector extends ConcurrentCollectorSingle <IndexerWorkItem>
  {
    public IndexerCollector ()
    {
      super (1_000_000);
      setPerformer (IndexerWorkQueue.getInstance ()::_fetchParticipantData);
    }
  }

  private final IndexerCollector m_aCollector = new IndexerCollector ();
  private final ThreadFactory m_aThreadFactory = new ExtendedDefaultThreadFactory ("IndexerWorkQueue");
  private final ExecutorService m_aSenderThreadPool = new ThreadPoolExecutor (1,
                                                                              1,
                                                                              60L,
                                                                              TimeUnit.SECONDS,
                                                                              new SynchronousQueue <Runnable> (),
                                                                              m_aThreadFactory);

  @Deprecated
  @UsedViaReflection
  public IndexerWorkQueue ()
  {
    // Start the collector
    m_aSenderThreadPool.submit (m_aCollector);
  }

  @Nonnull
  public static IndexerWorkQueue getInstance ()
  {
    return getGlobalSingleton (IndexerWorkQueue.class);
  }

  @Override
  protected void onDestroy (@Nonnull final IScope aScopeInDestruction)
  {
    // don't take any more actions
    m_aSenderThreadPool.shutdown ();
    m_aCollector.stopQueuingNewObjects ();
    ManagedExecutorService.shutdownAndWaitUntilAllTasksAreFinished (m_aSenderThreadPool);
  }

  /**
   * This is the main method to perform the operation on the SMP.
   *
   * @param aItem
   *        The item to be fetched. Never <code>null</code>.
   */
  private void _fetchParticipantData (@Nonnull final IndexerWorkItem aItem)
  {}

  public void queueObject (@Nonnull final IParticipantIdentifier aParticipantID,
                           @Nonnull final EIndexerWorkItemType eType)
  {
    final IndexerWorkItem aItem = new IndexerWorkItem (aParticipantID, eType);
    m_aCollector.queueObject (aItem);
  }
}
