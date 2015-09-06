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

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.concurrent.ExtendedDefaultThreadFactory;
import com.helger.commons.concurrent.ManagedExecutorService;
import com.helger.commons.concurrent.collector.ConcurrentCollectorSingle;
import com.helger.commons.microdom.IMicroDocument;
import com.helger.commons.microdom.IMicroElement;
import com.helger.commons.microdom.MicroDocument;
import com.helger.commons.microdom.convert.MicroTypeConverter;
import com.helger.commons.microdom.serialize.MicroReader;
import com.helger.commons.microdom.serialize.MicroWriter;
import com.helger.commons.scope.IScope;
import com.helger.commons.scope.singleton.AbstractGlobalSingleton;
import com.helger.peppol.identifier.IParticipantIdentifier;
import com.helger.photon.basic.app.io.WebFileIO;

public class IndexerWorkQueue extends AbstractGlobalSingleton
{
  private static final String ELEMENT_ROOT = "root";
  private static final String ELEMENT_ITEM = "item";
  private static final Logger s_aLogger = LoggerFactory.getLogger (IndexerWorkQueue.class);

  private final ConcurrentCollectorSingle <IndexerWorkItem> m_aImmediateCollector;
  private final ThreadFactory m_aThreadFactory = new ExtendedDefaultThreadFactory ("IndexerWorkQueue");
  private final ExecutorService m_aSenderThreadPool = new ThreadPoolExecutor (1,
                                                                              1,
                                                                              60L,
                                                                              TimeUnit.SECONDS,
                                                                              new SynchronousQueue <Runnable> (),
                                                                              m_aThreadFactory);

  @Nonnull
  private static File _getFile ()
  {
    return WebFileIO.getDataIO ().getFile ("indexer-work-queue.xml");
  }

  @Deprecated
  @UsedViaReflection
  public IndexerWorkQueue ()
  {
    m_aImmediateCollector = new ConcurrentCollectorSingle <IndexerWorkItem> (new LinkedBlockingQueue <> ());
    m_aImmediateCollector.setPerformer (this::_fetchParticipantData);

    // Read an eventually existing serialized element
    final IMicroDocument aDoc = MicroReader.readMicroXML (_getFile ());
    if (aDoc != null)
      for (final IMicroElement eItem : aDoc.getDocumentElement ().getAllChildElements (ELEMENT_ITEM))
      {
        final IndexerWorkItem aItem = MicroTypeConverter.convertToNative (eItem, IndexerWorkItem.class);
        m_aImmediateCollector.queueObject (aItem);
      }

    // Start the collector
    m_aSenderThreadPool.submit (m_aImmediateCollector);
  }

  @Nonnull
  public static IndexerWorkQueue getInstance ()
  {
    return getGlobalSingleton (IndexerWorkQueue.class);
  }

  private static void _write (@Nonnull final List <IndexerWorkItem> aItems)
  {
    if (!aItems.isEmpty ())
    {
      s_aLogger.info ("Persisting " + aItems.size () + " items");
      final IMicroDocument aDoc = new MicroDocument ();
      final IMicroElement eRoot = aDoc.appendElement (ELEMENT_ROOT);
      for (final IndexerWorkItem aItem : aItems)
        eRoot.appendChild (MicroTypeConverter.convertToMicroElement (aItem, ELEMENT_ITEM));
      if (MicroWriter.writeToFile (aDoc, _getFile ()).isFailure ())
        throw new IllegalStateException ("Failed to write IndexerWorkItems to " + _getFile ());
    }
  }

  @Override
  protected void onBeforeDestroy (@Nonnull final IScope aScopeToBeDestroyed)
  {
    // don't take any more actions
    m_aSenderThreadPool.shutdown ();
    m_aImmediateCollector.stopQueuingNewObjects ();

    // Get all remaining objects and save them for late reuse
    final List <IndexerWorkItem> aRemainingItems = m_aImmediateCollector.drainQueue ();
    _write (aRemainingItems);

    // Shutdown the thread pool
    ManagedExecutorService.shutdownAndWaitUntilAllTasksAreFinished (m_aSenderThreadPool);
  }

  /**
   * This is the main method to perform the operation on the SMP.
   *
   * @param aItem
   *        The item to be fetched. Never <code>null</code>.
   */
  private void _fetchParticipantData (@Nonnull final IndexerWorkItem aItem)
  {
    // TODO Perform SMP queries etc.
  }

  public void queueObject (@Nonnull final IParticipantIdentifier aParticipantID,
                           @Nonnull final EIndexerWorkItemType eType)
  {
    final IndexerWorkItem aItem = new IndexerWorkItem (aParticipantID, eType);
    m_aImmediateCollector.queueObject (aItem);
  }
}
