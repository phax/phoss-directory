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
package com.helger.pd.publisher.job;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsHashMap;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsMap;
import com.helger.pd.indexer.index.EIndexerWorkItemType;
import com.helger.pd.indexer.index.IIndexerWorkItem;
import com.helger.pd.indexer.mgr.PDIndexerManager;
import com.helger.pd.indexer.mgr.PDIndexerManager.BulkQueueResult;
import com.helger.pd.indexer.mgr.PDMetaManager;
import com.helger.pd.indexer.reindex.IReIndexWorkItem;
import com.helger.pd.indexer.reindex.IReIndexWorkItemList;
import com.helger.pd.indexer.storage.CPDStorage;
import com.helger.pd.publisher.app.AppCommonUI;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.photon.mgrs.longrun.LongRunningJobResult;
import com.helger.photon.security.lock.SingleRunLock;
import com.helger.text.ReadOnlyMultilingualText;

/**
 * A long running job that moves all entries of a re-index list or of a dead list back into the
 * indexing queue. Such a list may contain tens of thousands of entries, and queueing them one by
 * one scans the re-index and the dead list for every single entry - so this must not happen in an
 * HTTP thread.
 *
 * @author Philip Helger
 * @since 0.18.3
 */
public class PDReIndexAllJob extends AbstractPDLongRunningJob
{
  /** The type of this long running job */
  public static final String JOB_TYPE = "reindex-all";

  /**
   * The process wide lock ensuring, that only a single bulk re-index of the re-index list runs at a
   * time.
   */
  public static final SingleRunLock LOCK_REINDEX_LIST = new SingleRunLock ("Re-index list bulk re-index");

  /**
   * The process wide lock ensuring, that only a single bulk re-index of the dead list runs at a
   * time.
   */
  public static final SingleRunLock LOCK_DEAD_LIST = new SingleRunLock ("Dead list bulk re-index");

  private static final Logger LOGGER = LoggerFactory.getLogger (PDReIndexAllJob.class);

  private final IReIndexWorkItemList m_aWorkItemList;
  private final String m_sListName;

  public PDReIndexAllJob (@NonNull final IReIndexWorkItemList aWorkItemList,
                          @NonNull @Nonempty final String sListName,
                          @NonNull @Nonempty final String sUserID,
                          @NonNull final SingleRunLock aLock)
  {
    super (JOB_TYPE,
           new ReadOnlyMultilingualText (AppCommonUI.DEFAULT_LOCALE, "Re-index all entries of the " + sListName),
           sUserID,
           aLock);
    ValueEnforcer.notNull (aWorkItemList, "WorkItemList");
    ValueEnforcer.notEmpty (sListName, "ListName");
    m_aWorkItemList = aWorkItemList;
    m_sListName = sListName;
  }

  @Override
  @NonNull
  @ReturnsMutableCopy
  protected ICommonsList <Object> getAuditArgs ()
  {
    return new CommonsArrayList <> (m_sListName);
  }

  @Override
  @NonNull
  @ReturnsMutableCopy
  protected ICommonsList <Object> getAuditStartArgs ()
  {
    final ICommonsList <Object> ret = getAuditArgs ();
    ret.add (Integer.valueOf (m_aWorkItemList.getItemCount ()));
    return ret;
  }

  @Override
  @NonNull
  protected LongRunningJobResult createJobResult ()
  {
    // The returned list is a copy already - queueing the items modifies the source list
    final ICommonsList <? extends IReIndexWorkItem> aAllItems = m_aWorkItemList.getAllItems ();

    // A single bulk queueing handles exactly one work item type, so the participants are grouped
    // by the type of their work item first
    final ICommonsMap <EIndexerWorkItemType, ICommonsList <IParticipantIdentifier>> aPerType = new CommonsHashMap <> ();
    for (final IReIndexWorkItem aItem : aAllItems)
    {
      final IIndexerWorkItem aWorkItem = aItem.getWorkItem ();
      aPerType.computeIfAbsent (aWorkItem.getType (), _ -> new CommonsArrayList <IParticipantIdentifier> ())
              .add (aWorkItem.getParticipantID ());
    }

    LOGGER.info ("Re-indexing all " + aAllItems.size () + " entries of the " + m_sListName);

    final PDIndexerManager aIndexerMgr = PDMetaManager.getIndexerMgr ();
    final ICommonsList <String> aAlreadyQueued = new CommonsArrayList <> ();
    int nQueued = 0;
    for (final var aEntry : aPerType.entrySet ())
    {
      final BulkQueueResult aQueueResult = aIndexerMgr.queueWorkItems (aEntry.getValue (),
                                                                       aEntry.getKey (),
                                                                       CPDStorage.OWNER_MANUALLY_TRIGGERED,
                                                                       PDIndexerManager.HOST_LOCALHOST);
      nQueued += aQueueResult.getQueuedCount ();
      aAlreadyQueued.addAllMapped (aQueueResult.getAllNotQueued (), IParticipantIdentifier::getURIEncoded);
    }

    LOGGER.info ("Finished re-indexing all entries of the " +
                 m_sListName +
                 ". Queued " +
                 nQueued +
                 "; already in the indexing queue: " +
                 aAlreadyQueued.size ());

    final StringBuilder aSB = new StringBuilder ();
    aSB.append ("Source list: ").append (m_sListName).append ('\n');
    aSB.append ("Entries in the list: ").append (aAllItems.size ()).append ('\n');
    aSB.append ("Queued for re-indexing: ").append (nQueued).append ('\n');
    aSB.append ("Already in the indexing queue: ").append (aAlreadyQueued.size ()).append ('\n');

    // The participant IDs that were really queued are deliberately not logged one by one - such a
    // list may contain tens of thousands of entries
    appendDetails (aSB, "Already in the indexing queue", aAlreadyQueued);

    return LongRunningJobResult.createText (aSB.toString ());
  }
}
