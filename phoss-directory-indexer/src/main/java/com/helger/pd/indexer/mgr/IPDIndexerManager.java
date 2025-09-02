package com.helger.pd.indexer.mgr;

import java.io.Closeable;

import com.helger.annotation.Nonempty;
import com.helger.base.state.EChange;
import com.helger.pd.indexer.index.EIndexerWorkItemType;
import com.helger.pd.indexer.index.IndexerWorkItemQueue;
import com.helger.pd.indexer.reindex.IReIndexWorkItemList;
import com.helger.peppolid.IParticipantIdentifier;

import jakarta.annotation.Nonnull;

public interface IPDIndexerManager extends Closeable
{
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
  @Nonnull
  EChange queueWorkItem (@Nonnull IParticipantIdentifier aParticipantID,
                         @Nonnull EIndexerWorkItemType eType,
                         @Nonnull @Nonempty String sOwnerID,
                         @Nonnull @Nonempty String sRequestingHost);

  void expireOldEntries ();

  void reIndexParticipantData ();

  /**
   * @return The queue with all work items. Never <code>null</code> but maybe empty.
   */
  @Nonnull
  IndexerWorkItemQueue getIndexerWorkQueue ();

  /**
   * @return A list with all items where the re-index period has expired. Never <code>null</code>
   *         but maybe empty.
   */
  @Nonnull
  IReIndexWorkItemList getReIndexList ();

  /**
   * @return A list with all items where the re-index period has expired. Never <code>null</code>
   *         but maybe empty.
   */
  @Nonnull
  IReIndexWorkItemList getDeadList ();
}
