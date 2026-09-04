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
package com.helger.pd.indexer.mgr;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.GuardedBy;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.concurrent.SimpleReadWriteLock;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.EChange;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSet;
import com.helger.dao.DAOException;
import com.helger.datetime.helper.PDTFactory;
import com.helger.io.file.FileOperationManager;
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
import com.helger.pd.indexer.settings.PDServerConfiguration;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.photon.io.WebFileIO;
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
 * The global indexer manager that takes an item for queuing and maintains the uniqueness of the
 * items to queue.
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
  private final AtomicBoolean m_aIndexingStarted = new AtomicBoolean (false);

  /**
   * This set contains all work items that are not yet finished. It contains all items in the
   * indexer work queue as well as the ones in the re-index work item list. Once the items are moved
   * to the dead list, they are removed from here.
   */
  @GuardedBy ("m_aRWLock")
  private final ICommonsSet <IIndexerWorkItem> m_aUniqueItems = new CommonsHashSet <> ();

  // Status vars
  private final GlobalQuartzScheduler m_aScheduler;
  private volatile TriggerKey m_aTriggerKey;

  private void _onIndexSuccess (@NonNull final IIndexerWorkItem aWorkItem)
  {
    m_aRWLock.writeLocked (() -> m_aUniqueItems.remove (aWorkItem));
  }

  private void _onIndexFailure (@NonNull final IIndexerWorkItem aWorkItem,
                                @Nullable final ICommonsList <String> aErrorMsgs)
  {
    // if (PDServerConfiguration.getConfig ().getAsBoolean ("reindex.enabled", true))
    // Initially add to re-index list

    m_aReIndexList.addItem (new ReIndexWorkItem (aWorkItem, aErrorMsgs), true);
    // Keep it in the "Unique items" list until re-indexing worked
  }

  private void _onReIndexSuccess (@NonNull final IIndexerWorkItem aWorkItem)
  {
    _onIndexSuccess (aWorkItem);
  }

  private void _onReIndexFailure (@NonNull final IReIndexWorkItem aReIndexItem,
                                  @Nullable final ICommonsList <String> aErrorMsgs)
  {
    m_aReIndexList.incRetryCountAndAddItem (aReIndexItem, aErrorMsgs);
  }

  /**
   * Constructor.<br>
   * Initialized the work item queue, the re-index queue and the dead-queue.<br>
   * This constructor deliberately performs no indexing at all - call {@link #startIndexing()} for
   * that, as soon as all the prerequisites for indexing are fulfilled.
   *
   * @param aStorageMgr
   *        Storage manager to used. May not be <code>null</code>.
   * @throws DAOException
   *         If DAO initialization failed
   */
  public PDIndexerManager (@NonNull final IPDStorageManager aStorageMgr) throws DAOException
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
                                                                                                   this::_onIndexSuccess,
                                                                                                   this::_onIndexFailure),
                                                    PDServerConfiguration.getIndexerMaxParallel ());

    // remember here
    m_aScheduler = GlobalQuartzScheduler.getInstance ();
  }

  /**
   * Start all the indexing activities.<br>
   * Schedules the re-index job.<br>
   * Reads all work items persisted to disk and queues them again. This happens when the application
   * is shutdown while elements are still in the queue.<br>
   * Please note that the queuing of the items directly triggers the usage of the
   * {@link PDMetaManager#getBusinessCardProvider()} so make sure to call
   * {@link PDMetaManager#setBusinessCardProvider(IPDBusinessCardProvider)} before calling this
   * method. Calling this method more than once has no effect.
   *
   * @throws IllegalStateException
   *         If no {@link IPDBusinessCardProvider} is present.
   */
  public void startIndexing ()
  {
    // The queued work items are executed immediately, so the Business Card provider must already be
    // present
    if (PDMetaManager.getBusinessCardProviderOrNull () == null)
      throw new IllegalStateException ("The BusinessCard provider must be set before the indexing can be started");

    if (m_aIndexingStarted.getAndSet (true))
    {
      LOGGER.warn ("The indexing was already started - not starting it again");
      return;
    }

    // Schedule re-index job
    m_aTriggerKey = ReIndexJob.schedule (SimpleScheduleBuilder.repeatMinutelyForever (1));

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
      final IMicroElement eRoot = aDoc.addElement (ELEMENT_ROOT);
      for (final IIndexerWorkItem aItem : aRemainingWorkItems)
        eRoot.addChild (MicroTypeConverter.convertToMicroElement (aItem, ELEMENT_ITEM));
      if (MicroWriter.writeToFile (aDoc, m_aIndexerWorkItemFile).isFailure ())
        throw new IllegalStateException ("Failed to write IndexerWorkItems to " + m_aIndexerWorkItemFile);
    }

    // Unschedule the job to avoid problems on shutdown. Use the saved instance
    // because GlobalQuartzScheduler.getInstance() would fail because the global
    // scope is already in destruction.
    // The trigger key is only present if the indexing was started
    final TriggerKey aTriggerKey = m_aTriggerKey;
    if (aTriggerKey != null)
      m_aScheduler.unscheduleJob (aTriggerKey);

    // Close Lucene index etc.
    m_aStorageMgr.close ();
  }

  /**
   * The outcome of a bulk queueing operation - see
   * {@link PDIndexerManager#queueWorkItems(Collection, EIndexerWorkItemType, String, String)}.
   *
   * @author Philip Helger
   * @since 0.17.2
   */
  public static final class BulkQueueResult
  {
    private final ICommonsList <IParticipantIdentifier> m_aQueued;
    private final ICommonsList <IParticipantIdentifier> m_aNotQueued;

    BulkQueueResult (@NonNull final ICommonsList <IParticipantIdentifier> aQueued,
                     @NonNull final ICommonsList <IParticipantIdentifier> aNotQueued)
    {
      m_aQueued = aQueued;
      m_aNotQueued = aNotQueued;
    }

    /**
     * @return All participant IDs that were newly queued for indexing. Never <code>null</code>.
     */
    @NonNull
    @ReturnsMutableCopy
    public ICommonsList <IParticipantIdentifier> getAllQueued ()
    {
      return m_aQueued.getClone ();
    }

    /**
     * @return All participant IDs that were not queued, because they already were in the
     *         queue/re-index list. Never <code>null</code>.
     */
    @NonNull
    @ReturnsMutableCopy
    public ICommonsList <IParticipantIdentifier> getAllNotQueued ()
    {
      return m_aNotQueued.getClone ();
    }

    @Nonnegative
    public int getQueuedCount ()
    {
      return m_aQueued.size ();
    }

    @Nonnegative
    public int getNotQueuedCount ()
    {
      return m_aNotQueued.size ();
    }

    @Override
    public String toString ()
    {
      return new ToStringGenerator (this).append ("Queued", m_aQueued.size ())
                                         .append ("NotQueued", m_aNotQueued.size ())
                                         .getToString ();
    }
  }

  /**
   * Remove all the provided work items from the re-index and the dead list, so that the new items
   * don't spam the dead list. Both lists are scanned exactly once, no matter how many work items
   * are provided - a per-item lookup would scan each list from the start for every single item and
   * is therefore unusable for bulk operations.
   *
   * @param aWorkItems
   *        The work items that were newly queued. May not be <code>null</code>.
   */
  private void _removeFromOtherLists (@NonNull final Set <IIndexerWorkItem> aWorkItems)
  {
    final int nReIndex = m_aReIndexList.getAndRemoveAllEntries (x -> aWorkItems.contains (x.getWorkItem ())).size ();
    if (nReIndex > 0)
      LOGGER.info ("Removed " + nReIndex + " of the new work items from the re-index list");

    final int nDead = m_aDeadList.getAndRemoveAllEntries (x -> aWorkItems.contains (x.getWorkItem ())).size ();
    if (nDead > 0)
      LOGGER.info ("Removed " + nDead + " of the new work items from the dead list");
  }

  /**
   * Queue a single work item of any type, without touching the re-index and the dead list. If the
   * item is already in the queue, it is ignored.
   *
   * @param aWorkItem
   *        Work item to be queued. May not be <code>null</code>.
   * @param bLogSingleItems
   *        <code>true</code> to log every single item, <code>false</code> to stay silent. Bulk
   *        callers should pass <code>false</code> and log an aggregate instead.
   * @return {@link EChange#CHANGED} if it was queued
   */
  @NonNull
  private EChange _queueUniqueWorkItemNoCleanup (@NonNull final IIndexerWorkItem aWorkItem,
                                                 final boolean bLogSingleItems)
  {
    ValueEnforcer.notNull (aWorkItem, "WorkItem");

    // Check for duplicate
    m_aRWLock.writeLock ().lock ();
    try
    {
      if (!m_aUniqueItems.add (aWorkItem))
      {
        if (bLogSingleItems)
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
    if (m_aIndexerWorkQueue.queueObject (aWorkItem).isFailure ())
    {
      LOGGER.error ("Failed to queue work item " + aWorkItem.getLogText ());
      return EChange.UNCHANGED;
    }
    if (bLogSingleItems)
      LOGGER.info ("Queued work item " + aWorkItem.getLogText ());

    return EChange.CHANGED;
  }

  /**
   * Queue a single work item of any type. If the item is already in the queue, it is ignored.
   *
   * @param aWorkItem
   *        Work item to be queued. May not be <code>null</code>.
   * @return {@link EChange#CHANGED} if it was queued
   */
  @NonNull
  private EChange _queueUniqueWorkItem (@NonNull final IIndexerWorkItem aWorkItem)
  {
    final EChange ret = _queueUniqueWorkItemNoCleanup (aWorkItem, true);
    if (ret.isChanged ())
    {
      // Remove the entry from the other lists to avoid spamming the dead list
      _removeFromOtherLists (new CommonsHashSet <> (aWorkItem));
    }
    return ret;
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
   * @return {@link EChange#UNCHANGED} if the item was queued, {@link EChange#UNCHANGED} if this
   *         item is already in the queue!
   */
  @NonNull
  public EChange queueWorkItem (@NonNull final IParticipantIdentifier aParticipantID,
                                @NonNull final EIndexerWorkItemType eType,
                                @NonNull @Nonempty final String sOwnerID,
                                @NonNull @Nonempty final String sRequestingHost)
  {
    // Build item
    final IIndexerWorkItem aWorkItem = new IndexerWorkItem (aParticipantID, eType, sOwnerID, sRequestingHost);
    // And queue it
    return _queueUniqueWorkItem (aWorkItem);
  }

  /**
   * Queue a lot of work items of the same type at once. Contrary to calling
   * {@link #queueWorkItem(IParticipantIdentifier, EIndexerWorkItemType, String, String)} in a loop,
   * the re-index and the dead list are cleaned up in a single pass at the end, and only an
   * aggregate is logged. That makes this method usable for tens of thousands of participants.
   *
   * @param aParticipantIDs
   *        The participant IDs to be queued. May not be <code>null</code>. Duplicates in here are
   *        reported as "not queued".
   * @param eType
   *        Action type.
   * @param sOwnerID
   *        Owner of this action
   * @param sRequestingHost
   *        Requesting host (IP address)
   * @return Never <code>null</code>.
   * @since 0.17.2
   */
  @NonNull
  public BulkQueueResult queueWorkItems (@NonNull final Collection <? extends IParticipantIdentifier> aParticipantIDs,
                                         @NonNull final EIndexerWorkItemType eType,
                                         @NonNull @Nonempty final String sOwnerID,
                                         @NonNull @Nonempty final String sRequestingHost)
  {
    ValueEnforcer.notNull (aParticipantIDs, "ParticipantIDs");
    ValueEnforcer.notNull (eType, "Type");
    ValueEnforcer.notEmpty (sOwnerID, "OwnerID");
    ValueEnforcer.notEmpty (sRequestingHost, "RequestingHost");

    LOGGER.info ("Bulk queueing " +
                 aParticipantIDs.size () +
                 " work items of type " +
                 eType.getID () +
                 " for owner '" +
                 sOwnerID +
                 "'");

    final ICommonsList <IParticipantIdentifier> aQueued = new CommonsArrayList <> ();
    final ICommonsList <IParticipantIdentifier> aNotQueued = new CommonsArrayList <> ();
    final ICommonsSet <IIndexerWorkItem> aQueuedItems = new CommonsHashSet <> ();

    for (final IParticipantIdentifier aParticipantID : aParticipantIDs)
    {
      final IIndexerWorkItem aWorkItem = new IndexerWorkItem (aParticipantID, eType, sOwnerID, sRequestingHost);
      if (_queueUniqueWorkItemNoCleanup (aWorkItem, false).isChanged ())
      {
        aQueued.add (aParticipantID);
        aQueuedItems.add (aWorkItem);
      }
      else
        aNotQueued.add (aParticipantID);
    }

    if (aQueuedItems.isNotEmpty ())
    {
      // Remove the new entries from the other lists to avoid spamming the dead list
      _removeFromOtherLists (aQueuedItems);
    }

    LOGGER.info ("Finished bulk queueing. Queued " +
                 aQueued.size () +
                 "; already in the queue: " +
                 aNotQueued.size ());

    return new BulkQueueResult (aQueued, aNotQueued);
  }

  /**
   * Expire all re-index entries that are in the list for a too long time. This is called from a
   * scheduled job only. All respective items are move from the re-index list to the dead list.
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
        m_aRWLock.writeLocked (() -> m_aUniqueItems.remove (aItem.getWorkItem ()));

        // move all to the dead item list
        m_aDeadList.addItem ((ReIndexWorkItem) aItem, false);
        LOGGER.info ("Added " + aItem.getLogText () + " to the dead list");
      }
    }
  }

  /**
   * Re-index all entries that are ready to be re-indexed now. This is called from a scheduled job
   * only.
   */
  public void reIndexParticipantDataSynchronously ()
  {
    final LocalDateTime aNow = PDTFactory.getCurrentLocalDateTime ();

    // Get and remove all items to re-index "now"
    final ICommonsList <IReIndexWorkItem> aReIndexNowItems = m_aReIndexList.getAndRemoveAllEntries (aWorkItem -> aWorkItem.isRetryPossible (aNow));

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Re-indexing " + aReIndexNowItems.size () + " work items");

    for (final IReIndexWorkItem aReIndexItem : aReIndexNowItems)
    {
      LOGGER.info ("Try to re-index " + aReIndexItem.getLogText ());

      PDIndexExecutor.executeWorkItem (m_aStorageMgr,
                                       aReIndexItem.getWorkItem (),
                                       1 + aReIndexItem.getRetryCount (),
                                       this::_onReIndexSuccess,
                                       (_, aErrorMsgs) -> _onReIndexFailure (aReIndexItem, aErrorMsgs));
    }
  }

  /**
   * @return The queue with all work items. Never <code>null</code> but maybe empty.
   */
  @NonNull
  public IndexerWorkItemQueue getIndexerWorkQueue ()
  {
    return m_aIndexerWorkQueue;
  }

  /**
   * @return A list with all items where the re-index period has expired. Never <code>null</code>
   *         but maybe empty.
   */
  @NonNull
  public IReIndexWorkItemList getReIndexList ()
  {
    return m_aReIndexList;
  }

  /**
   * @return A list with all items where the re-index period has expired. Never <code>null</code>
   *         but maybe empty.
   */
  @NonNull
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
