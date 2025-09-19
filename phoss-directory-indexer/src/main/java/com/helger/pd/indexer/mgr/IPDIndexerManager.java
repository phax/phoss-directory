/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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
