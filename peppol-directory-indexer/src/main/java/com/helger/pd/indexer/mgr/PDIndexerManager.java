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
package com.helger.pd.indexer.mgr;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;

import org.joda.time.LocalDateTime;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.commons.microdom.IMicroDocument;
import com.helger.commons.microdom.IMicroElement;
import com.helger.commons.microdom.MicroDocument;
import com.helger.commons.microdom.convert.MicroTypeConverter;
import com.helger.commons.microdom.serialize.MicroReader;
import com.helger.commons.microdom.serialize.MicroWriter;
import com.helger.commons.state.EChange;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.ToStringGenerator;
import com.helger.datetime.PDTFactory;
import com.helger.pd.businessinformation.IPDBusinessInformationProvider;
import com.helger.pd.businessinformation.PDExtendedBusinessInformation;
import com.helger.pd.indexer.domain.EIndexerWorkItemType;
import com.helger.pd.indexer.domain.IndexerWorkItem;
import com.helger.pd.indexer.domain.ReIndexWorkItem;
import com.helger.pd.indexer.job.ReIndexJob;
import com.helger.pd.indexer.storage.PDStorageManager;
import com.helger.peppol.identifier.IParticipantIdentifier;
import com.helger.peppol.identifier.participant.IPeppolParticipantIdentifier;
import com.helger.photon.basic.app.dao.impl.DAOException;
import com.helger.photon.basic.app.io.WebFileIO;
import com.helger.photon.core.app.CApplication;
import com.helger.schedule.quartz.GlobalQuartzScheduler;

/**
 * The global indexer manager that takes an item for queuing and maintains the
 * uniqueness of the items to queue.
 *
 * @author Philip Helger
 */
public final class PDIndexerManager implements Closeable
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (PDIndexerManager.class);
  private static final String ELEMENT_ROOT = "root";
  private static final String ELEMENT_ITEM = "item";

  private final SimpleReadWriteLock m_aRWLock = new SimpleReadWriteLock ();
  private final PDStorageManager m_aStorageMgr;
  private final File m_aIndexerWorkItemFile;
  private final IndexerWorkItemQueue m_aIndexerWorkQueue = new IndexerWorkItemQueue (this::_asyncFetchParticipantData);
  private final ReIndexWorkItemList m_aReIndexList;
  private final ReIndexWorkItemList m_aDeadList;
  private final TriggerKey m_aTriggerKey;
  @GuardedBy ("m_aRWLock")
  private final Set <IndexerWorkItem> m_aUniqueItems = new HashSet <> ();
  @GuardedBy ("m_aRWLock")
  private IPDBusinessInformationProvider m_aBIProvider = new SMPBusinessInformationProvider ();

  // Status vars
  private final GlobalQuartzScheduler m_aScheduler;

  public PDIndexerManager (@Nonnull final PDStorageManager aStorageMgr) throws DAOException
  {
    m_aStorageMgr = ValueEnforcer.notNull (aStorageMgr, "StorageMgr");
    m_aReIndexList = new ReIndexWorkItemList ("reindex-work-items.xml");
    m_aDeadList = new ReIndexWorkItemList ("dead-work-items.xml");

    // Remember the file because upon shutdown WebFileIO may already be
    // discarded
    m_aIndexerWorkItemFile = WebFileIO.getDataIO ().getFile ("indexer-work-items.xml");

    // Schedule re-index job
    m_aTriggerKey = ReIndexJob.schedule (SimpleScheduleBuilder.repeatMinutelyForever (1), CApplication.APP_ID_SECURE);

    // remember here
    m_aScheduler = GlobalQuartzScheduler.getInstance ();
  }

  /**
   * Read all work items persisted to disk. This happens when the application is
   * shutdown while elements are still in the queue. This should be called
   * directly after the constructor. But please note that the queuing of the
   * items might directly trigger the usage of the
   * {@link #getBusinessInformationProvider()} so make sure to call
   * {@link #setBusinessInformationProvider(IPDBusinessInformationProvider)}
   * before calling this method.
   *
   * @return this for chaining
   */
  @Nonnull
  public PDIndexerManager readAndQueueInitialData ()
  {
    // Read the file - may not be existing
    final IMicroDocument aDoc = MicroReader.readMicroXML (m_aIndexerWorkItemFile);
    if (aDoc != null)
    {
      if (s_aLogger.isDebugEnabled ())
        s_aLogger.debug ("Reading persisted indexer work items from " + m_aIndexerWorkItemFile);
      for (final IMicroElement eItem : aDoc.getDocumentElement ().getAllChildElements (ELEMENT_ITEM))
      {
        final IndexerWorkItem aWorkItem = MicroTypeConverter.convertToNative (eItem, IndexerWorkItem.class);
        _queueUniqueWorkItem (aWorkItem);
      }

      // Delete the files to ensure it is not read again next startup time
      WebFileIO.getFileOpMgr ().deleteFile (m_aIndexerWorkItemFile);
    }
    return this;
  }

  /**
   * Write all work items to a file.
   *
   * @param aItems
   *        The items to be written. May not be <code>null</code> but maybe
   *        empty.
   */
  private void _writeWorkItems (@Nonnull final List <IndexerWorkItem> aItems)
  {
    if (!aItems.isEmpty ())
    {
      s_aLogger.info ("Persisting " + aItems.size () + " indexer work items");
      final IMicroDocument aDoc = new MicroDocument ();
      final IMicroElement eRoot = aDoc.appendElement (ELEMENT_ROOT);
      for (final IndexerWorkItem aItem : aItems)
        eRoot.appendChild (MicroTypeConverter.convertToMicroElement (aItem, ELEMENT_ITEM));
      if (MicroWriter.writeToFile (aDoc, m_aIndexerWorkItemFile).isFailure ())
        throw new IllegalStateException ("Failed to write IndexerWorkItems to " + m_aIndexerWorkItemFile);
    }
  }

  public void close () throws IOException
  {
    // Get all remaining objects and save them for late reuse
    final List <IndexerWorkItem> aRemainingWorkItems = m_aIndexerWorkQueue.stop ();
    _writeWorkItems (aRemainingWorkItems);

    // Unschedule the job to avoid problems on shutdown. Use the saved instance
    // because GlobalQuartzScheduler.getInstance() would fail because the global
    // scope is already in destruction.
    m_aScheduler.unscheduleJob (m_aTriggerKey);

    // Close Lucene index etc.
    m_aStorageMgr.close ();
  }

  /**
   * @return The global {@link IPDBusinessInformationProvider}. Never
   *         <code>null</code>.
   */
  @Nonnull
  public IPDBusinessInformationProvider getBusinessInformationProvider ()
  {
    return m_aRWLock.readLocked ( () -> m_aBIProvider);
  }

  /**
   * Set the global {@link IPDBusinessInformationProvider} that is used for
   * future create/update requests.
   *
   * @param aBIProvider
   *        Business information provider to be used. May not be
   *        <code>null</code>.
   * @return this for chaining
   */
  @Nonnull
  public PDIndexerManager setBusinessInformationProvider (@Nonnull final IPDBusinessInformationProvider aBIProvider)
  {
    ValueEnforcer.notNull (aBIProvider, "BIProvider");
    m_aRWLock.writeLocked ( () -> m_aBIProvider = aBIProvider);
    return this;
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
  private EChange _queueUniqueWorkItem (@Nonnull final IndexerWorkItem aWorkItem)
  {
    ValueEnforcer.notNull (aWorkItem, "WorkItem");

    // Check for duplicate
    m_aRWLock.writeLock ().lock ();
    try
    {
      if (!m_aUniqueItems.add (aWorkItem))
      {
        s_aLogger.info ("Ignoring work item " + aWorkItem.getLogText () + " because it is already in the queue!");
        return EChange.UNCHANGED;
      }
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }

    // Queue it
    m_aIndexerWorkQueue.queueObject (aWorkItem);

    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug ("Queued work item " + aWorkItem.getLogText ());

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
    final IndexerWorkItem aWorkItem = new IndexerWorkItem (aParticipantID, eType, sOwnerID, sRequestingHost);
    // And queue it
    return _queueUniqueWorkItem (aWorkItem);
  }

  /**
   * Main action to create or update the business information of a participant.
   * Here the business information is retrieved and put into the Lucene index.
   *
   * @param aWorkItem
   *        Work item to execute.
   * @return {@link ESuccess}
   * @throws IOException
   *         On Lucene error
   */
  @Nonnull
  private ESuccess _executeCreateOrUpdate (@Nonnull final IndexerWorkItem aWorkItem) throws IOException
  {
    final IPeppolParticipantIdentifier aParticipantID = aWorkItem.getParticipantID ();

    // Get BI from participant
    final PDExtendedBusinessInformation aBI = getBusinessInformationProvider ().getBusinessInformation (aParticipantID);
    if (aBI == null)
    {
      // No/invalid extension present - no need to try again
      return ESuccess.FAILURE;
    }

    // Got data - put in storage
    return m_aStorageMgr.createOrUpdateEntry (aParticipantID, aBI, aWorkItem.getAsMetaData ());
  }

  /**
   * Main action to delete the business information of a participant. Here the
   * business information is removed from the Lucene index.
   *
   * @param aWorkItem
   *        Work item to execute.
   * @return {@link ESuccess}
   * @throws IOException
   *         On Lucene error
   */
  @Nonnull
  private ESuccess _executeDelete (@Nonnull final IndexerWorkItem aWorkItem) throws IOException
  {
    final IPeppolParticipantIdentifier aParticipantID = aWorkItem.getParticipantID ();

    return m_aStorageMgr.deleteEntry (aParticipantID, aWorkItem.getAsMetaData ());
  }

  /**
   * This method is responsible for executing the specified work item depending
   * on its type.
   *
   * @param aWorkItem
   *        The work item to be executed. May not be <code>null</code>.
   * @return {@link ESuccess}
   */
  @Nonnull
  private ESuccess _executeWorkItem (@Nonnull final IndexerWorkItem aWorkItem)
  {
    s_aLogger.info ("Execute " + aWorkItem.getLogText ());

    try
    {
      ESuccess eSuccess;
      switch (aWorkItem.getType ())
      {
        case CREATE_UPDATE:
          eSuccess = _executeCreateOrUpdate (aWorkItem);
          break;
        case DELETE:
          eSuccess = _executeDelete (aWorkItem);
          break;
        default:
          throw new IllegalStateException ("Unsupported item type: " + aWorkItem);
      }

      if (eSuccess.isSuccess ())
      {
        // Item handled - remove from overall list
        m_aRWLock.writeLocked ((Runnable) () -> m_aUniqueItems.remove (aWorkItem));

        // And we're done
        return ESuccess.SUCCESS;
      }

      // else error storing data
    }
    catch (final Exception ex)
    {
      s_aLogger.error ("Error in executing work item " + aWorkItem.getLogText (), ex);
      // Fall through
    }

    return ESuccess.FAILURE;
  }

  /**
   * This is the performer for the direct data fetching.
   *
   * @param aItem
   *        The item to be fetched. Never <code>null</code>.
   * @return {@link ESuccess}.
   */
  @Nonnull
  private ESuccess _asyncFetchParticipantData (@Nonnull final IndexerWorkItem aItem)
  {
    final ESuccess eSuccess = _executeWorkItem (aItem);

    if (eSuccess.isFailure ())
    {
      s_aLogger.warn ("Error fetching " + aItem.getLogText ());
      // Failed to fetch participant data - add to re-index queue and leave in
      // the overall list
      m_aReIndexList.addItem (new ReIndexWorkItem (aItem));
    }
    return eSuccess;
  }

  /**
   * Expire all re-index entries that are in the list for a too long time.
   */
  public void expireOldEntries ()
  {
    // Expire old entries
    final List <ReIndexWorkItem> aExpiredItems = m_aReIndexList.getAndRemoveAllEntries (aWorkItem -> aWorkItem.isExpired ());
    if (!aExpiredItems.isEmpty ())
    {
      s_aLogger.info ("Expiring " + aExpiredItems.size () + " re-index work items");

      m_aRWLock.writeLocked ( () -> {
        // remove them from the overall list but move to dead item list
        for (final ReIndexWorkItem aItem : aExpiredItems)
        {
          m_aUniqueItems.remove (aItem.getWorkItem ());
          m_aDeadList.addItem (aItem);
        }
      });
    }
  }

  /**
   * Re-index all entries that are ready to be re-indexed now.
   */
  public void reIndexParticipantData ()
  {
    // Get and remove all items to re-index "now"
    final LocalDateTime aNow = PDTFactory.getCurrentLocalDateTime ();
    final List <ReIndexWorkItem> aReIndexNowItems = m_aReIndexList.getAndRemoveAllEntries (aWorkItem -> aWorkItem.isRetryPossible (aNow));

    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug ("Re-indexing " + aReIndexNowItems.size () + " work items");

    for (final ReIndexWorkItem aReIndexItem : aReIndexNowItems)
    {
      if (s_aLogger.isDebugEnabled ())
        s_aLogger.debug ("Try to re-index " + aReIndexItem.getLogText ());

      if (_executeWorkItem (aReIndexItem.getWorkItem ()).isFailure ())
      {
        // Still no success. Add again to the retry list
        m_aReIndexList.incRetryCountAndAddItem (aReIndexItem);
      }
    }
  }

  /**
   * @return A list with all items where the re-index period has expired. Never
   *         <code>null</code> but maybe empty.
   */
  @Nonnull
  @ReturnsMutableCopy
  public List <ReIndexWorkItem> getAllDeadListItems ()
  {
    return m_aDeadList.getAllItems ();
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
                            .append ("BIProvider", m_aBIProvider)
                            .toString ();
  }
}
