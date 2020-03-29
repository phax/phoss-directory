/**
 * Copyright (C) 2015-2020 Philip Helger (www.helger.com)
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
package com.helger.pd.indexer.mgr;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.CommonsHashSet;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsSet;
import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.io.file.FileOperationManager;
import com.helger.commons.state.EChange;
import com.helger.commons.string.ToStringGenerator;
import com.helger.dao.DAOException;
import com.helger.pd.indexer.businesscard.IPDBusinessCardProvider;
import com.helger.pd.indexer.index.EIndexerWorkItemType;
import com.helger.pd.indexer.index.IIndexerWorkItem;
import com.helger.pd.indexer.index.IndexerWorkItem;
import com.helger.pd.indexer.index.IndexerWorkItemQueue;
import com.helger.pd.indexer.job.ReIndexJob;
import com.helger.pd.indexer.reindex.IReIndexWorkItem;
import com.helger.pd.indexer.reindex.IReIndexWorkItemList;
import com.helger.pd.indexer.reindex.ReIndexWorkItem;
import com.helger.pd.indexer.reindex.ReIndexWorkItemList;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.photon.app.io.WebFileIO;
import com.helger.quartz.SimpleScheduleBuilder;
import com.helger.quartz.TriggerKey;
import com.helger.schedule.quartz.GlobalQuartzScheduler;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroDocument;
import com.helger.xml.microdom.convert.MicroTypeConverter;
import com.helger.xml.microdom.serialize.MicroReader;
import com.helger.xml.microdom.serialize.MicroWriter;

/**
 * The global indexer manager that takes an item for queuing and maintains the
 * uniqueness of the items to queue.
 *
 * @author Philip Helger
 */
public final class PDIndexerManager implements Closeable
{
  public static final String HOST_LOCALHOST = "localhost";

  private static final Logger LOGGER = LoggerFactory.getLogger (PDIndexerManager.class);
  private static final String ELEMENT_ROOT = "root";
  private static final String ELEMENT_ITEM = "item";

  private final SimpleReadWriteLock m_aRWLock = new SimpleReadWriteLock ();
  private final IPDStorageManager m_aStorageMgr;
  private final File m_aIndexerWorkItemFile;
  private final ReIndexWorkItemList m_aReIndexList;
  private final ReIndexWorkItemList m_aDeadList;
  private final IndexerWorkItemQueue m_aIndexerWorkQueue;
  private final TriggerKey m_aTriggerKey;

  /**
   * This set contains all work items that are not yet finished. It contains all
   * items in the indexer work queue as well as the ones in the re-index work
   * item list. Once the items are moved to the dead list, they are removed from
   * here.
   */
  @GuardedBy ("m_aRWLock")
  private final ICommonsSet <IIndexerWorkItem> m_aUniqueItems = new CommonsHashSet <> ();

  // Status vars
  private final GlobalQuartzScheduler m_aScheduler;

  private void _onIndexSuccess (@Nonnull final IIndexerWorkItem aWorkItem)
  {
    m_aRWLock.writeLockedBoolean ( () -> m_aUniqueItems.remove (aWorkItem));
  }

  private void _onIndexFailure (@Nonnull final IIndexerWorkItem aWorkItem)
  {
    m_aReIndexList.addItem (new ReIndexWorkItem (aWorkItem));
    // Keep it in the "Unique items" list until re-indexing worked
  }

  private void _onReIndexSuccess (@Nonnull final IIndexerWorkItem aWorkItem)
  {
    _onIndexSuccess (aWorkItem);
  }

  private void _onReIndexFailure (@Nonnull final IReIndexWorkItem aReIndexItem)
  {
    m_aReIndexList.incRetryCountAndAddItem (aReIndexItem);
  }

  /**
   * Constructor.<br>
   * Initialized the work item queue, the re-index queue and the dead-queue.<br>
   * Schedules the re-index job.<br>
   * Read all work items persisted to disk. This happens when the application is
   * shutdown while elements are still in the queue.<br>
   * Please note that the queuing of the items might directly trigger the usage
   * of the {@link PDMetaManager#getBusinessCardProvider()} so make sure to call
   * {@link PDMetaManager#setBusinessCardProvider(IPDBusinessCardProvider)}
   * before calling this method.
   *
   * @param aStorageMgr
   *        Storage manager to used. May not be <code>null</code>.
   * @throws DAOException
   *         If DAO initialization failed
   */
  public PDIndexerManager (@Nonnull final IPDStorageManager aStorageMgr) throws DAOException
  {
    m_aStorageMgr = ValueEnforcer.notNull (aStorageMgr, "StorageMgr");

    // Remember the file because upon shutdown WebFileIO may already be
    // discarded
    m_aIndexerWorkItemFile = WebFileIO.getDataIO ().getFile ("indexer-work-items.xml");

    // Re-index list
    m_aReIndexList = new ReIndexWorkItemList ("reindex-work-items.xml");
    // Dead list
    m_aDeadList = new ReIndexWorkItemList ("dead-work-items.xml");

    // Main worker to perform the jobs
    m_aIndexerWorkQueue = new IndexerWorkItemQueue (aQueueItem -> PDIndexExecutor.executeWorkItem (m_aStorageMgr,
                                                                                                   aQueueItem,
                                                                                                   0,
                                                                                                   aSuccessItem -> _onIndexSuccess (aSuccessItem),
                                                                                                   aFailureItem -> _onIndexFailure (aFailureItem)));

    // Schedule re-index job
    m_aTriggerKey = ReIndexJob.schedule (SimpleScheduleBuilder.repeatMinutelyForever (1));

    // remember here
    m_aScheduler = GlobalQuartzScheduler.getInstance ();

    // Read the file - may not be existing
    final IMicroDocument aDoc = MicroReader.readMicroXML (m_aIndexerWorkItemFile);
    if (aDoc != null)
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Reading persisted indexer work items from " + m_aIndexerWorkItemFile);

      for (final IMicroElement eItem : aDoc.getDocumentElement ().getAllChildElements (ELEMENT_ITEM))
      {
        final IIndexerWorkItem aWorkItem = MicroTypeConverter.convertToNative (eItem, IndexerWorkItem.class);
        _queueUniqueWorkItem (aWorkItem);
      }

      // Delete the files to ensure it is not read again next startup time
      FileOperationManager.INSTANCE.deleteFile (m_aIndexerWorkItemFile);
    }
  }

  public void close () throws IOException
  {
    // Get all remaining objects and save them for late reuse
    final ICommonsList <IIndexerWorkItem> aRemainingWorkItems = m_aIndexerWorkQueue.stop ();
    if (aRemainingWorkItems.isNotEmpty ())
    {
      LOGGER.info ("Persisting " + aRemainingWorkItems.size () + " indexer work items");
      final IMicroDocument aDoc = new MicroDocument ();
      final IMicroElement eRoot = aDoc.appendElement (ELEMENT_ROOT);
      for (final IIndexerWorkItem aItem : aRemainingWorkItems)
        eRoot.appendChild (MicroTypeConverter.convertToMicroElement (aItem, ELEMENT_ITEM));
      if (MicroWriter.writeToFile (aDoc, m_aIndexerWorkItemFile).isFailure ())
        throw new IllegalStateException ("Failed to write IndexerWorkItems to " + m_aIndexerWorkItemFile);
    }

    // Unschedule the job to avoid problems on shutdown. Use the saved instance
    // because GlobalQuartzScheduler.getInstance() would fail because the global
    // scope is already in destruction.
    m_aScheduler.unscheduleJob (m_aTriggerKey);

    // Close Lucene index etc.
    m_aStorageMgr.close ();
  }

  /**
   * Queue a single work item of any type. If the item is already in the queue,
   * it is ignored.
   *
   * @param aWorkItem
   *        Work item to be queued. May not be <code>null</code>.
   * @return {@link EChange#CHANGED} if it was queued
   */
  @Nonnull
  private EChange _queueUniqueWorkItem (@Nonnull final IIndexerWorkItem aWorkItem)
  {
    ValueEnforcer.notNull (aWorkItem, "WorkItem");

    // Check for duplicate
    m_aRWLock.writeLock ().lock ();
    try
    {
      if (!m_aUniqueItems.add (aWorkItem))
      {
        LOGGER.info ("Ignoring work item " +
                     aWorkItem.getLogText () +
                     " because it is already in the queue/re-index list!");
        return EChange.UNCHANGED;
      }
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }

    // Queue it
    m_aIndexerWorkQueue.queueObject (aWorkItem);
    LOGGER.info ("Queued work item " + aWorkItem.getLogText ());

    // Remove the entry from the dead list to avoid spamming the dead list
    if (m_aDeadList.getAndRemoveEntry (x -> x.getWorkItem ().equals (aWorkItem)) != null)
      LOGGER.info ("Removed the new work item " + aWorkItem.getLogText () + " from the dead list");

    return EChange.CHANGED;
  }

  /**
   * Queue a new work item
   *
   * @param aParticipantID
   *        Participant ID to use.
   * @param eType
   *        Action type.
   * @param sOwnerID
   *        Owner of this action
   * @param sRequestingHost
   *        Requesting host (IP address)
   * @return {@link EChange#UNCHANGED} if the item was queued,
   *         {@link EChange#UNCHANGED} if this item is already in the queue!
   */
  @Nonnull
  public EChange queueWorkItem (@Nonnull final IParticipantIdentifier aParticipantID,
                                @Nonnull final EIndexerWorkItemType eType,
                                @Nonnull @Nonempty final String sOwnerID,
                                @Nonnull @Nonempty final String sRequestingHost)
  {
    // Build item
    final IIndexerWorkItem aWorkItem = new IndexerWorkItem (aParticipantID, eType, sOwnerID, sRequestingHost);
    // And queue it
    return _queueUniqueWorkItem (aWorkItem);
  }

  /**
   * Expire all re-index entries that are in the list for a too long time. This
   * is called from a scheduled job only. All respective items are move from the
   * re-index list to the dead list.
   */
  public void expireOldEntries ()
  {
    // Expire old entries
    final ICommonsList <IReIndexWorkItem> aExpiredItems = m_aReIndexList.getAndRemoveAllEntries (IReIndexWorkItem::isExpired);
    if (aExpiredItems.isNotEmpty ())
    {
      LOGGER.info ("Expiring " + aExpiredItems.size () + " re-index work items and move them to the dead list");

      for (final IReIndexWorkItem aItem : aExpiredItems)
      {
        // remove them from the overall list but move to dead item list
        m_aRWLock.writeLockedBoolean ( () -> m_aUniqueItems.remove (aItem.getWorkItem ()));

        // move all to the dead item list
        m_aDeadList.addItem ((ReIndexWorkItem) aItem);
        LOGGER.info ("Added " + aItem.getLogText () + " to the dead list");
      }
    }
  }

  /**
   * Re-index all entries that are ready to be re-indexed now. This is called
   * from a scheduled job only.
   */
  public void reIndexParticipantData ()
  {
    final LocalDateTime aNow = PDTFactory.getCurrentLocalDateTime ();

    // Get and remove all items to re-index "now"
    final List <IReIndexWorkItem> aReIndexNowItems = m_aReIndexList.getAndRemoveAllEntries (aWorkItem -> aWorkItem.isRetryPossible (aNow));

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Re-indexing " + aReIndexNowItems.size () + " work items");

    for (final IReIndexWorkItem aReIndexItem : aReIndexNowItems)
    {
      LOGGER.info ("Try to re-index " + aReIndexItem.getLogText ());

      PDIndexExecutor.executeWorkItem (m_aStorageMgr,
                                       aReIndexItem.getWorkItem (),
                                       1 + aReIndexItem.getRetryCount (),
                                       aSuccessItem -> _onReIndexSuccess (aSuccessItem),
                                       aFailureItem -> _onReIndexFailure (aReIndexItem));
    }
  }

  /**
   * @return The queue with all work items. Never <code>null</code> but maybe
   *         empty.
   */
  @Nonnull
  public IndexerWorkItemQueue getIndexerWorkQueue ()
  {
    return m_aIndexerWorkQueue;
  }

  /**
   * @return A list with all items where the re-index period has expired. Never
   *         <code>null</code> but maybe empty.
   */
  @Nonnull
  public IReIndexWorkItemList getReIndexList ()
  {
    return m_aReIndexList;
  }

  /**
   * @return A list with all items where the re-index period has expired. Never
   *         <code>null</code> but maybe empty.
   */
  @Nonnull
  public IReIndexWorkItemList getDeadList ()
  {
    return m_aDeadList;
  }

  @Override
  public String toString ()
  {
    return ToStringGenerator.getDerived (super.toString ())
                            .append ("UniqueItems", m_aUniqueItems)
                            .append ("ReIndexList", m_aReIndexList)
                            .append ("DeadList", m_aDeadList)
                            .append ("IndexerWorkQueue", m_aIndexerWorkQueue)
                            .append ("TriggerKey", m_aTriggerKey)
                            .getToString ();
  }
}
