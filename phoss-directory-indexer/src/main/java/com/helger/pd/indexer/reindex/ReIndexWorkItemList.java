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
package com.helger.pd.indexer.reindex;

import java.util.function.Predicate;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.dao.DAOException;
import com.helger.photon.io.dao.AbstractPhotonMapBasedWALDAO;

import jakarta.annotation.Nullable;

/**
 * This is the list with {@link IReIndexWorkItem} objects. It is solely used in
 * the {@link com.helger.pd.indexer.mgr.PDIndexerManager} for "re-index" and
 * "dead" work items.
 *
 * @author Philip Helger
 */
@ThreadSafe
public final class ReIndexWorkItemList extends AbstractPhotonMapBasedWALDAO <IReIndexWorkItem, ReIndexWorkItem> implements
                                       IReIndexWorkItemList
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ReIndexWorkItemList.class);

  public ReIndexWorkItemList (@Nullable final String sFilename) throws DAOException
  {
    super (ReIndexWorkItem.class, sFilename);
  }

  /**
   * Add a unique item to the list.
   *
   * @param aItem
   *        The item to be added. May not be <code>null</code>.
   * @throws IllegalStateException
   *         If an item with the same ID is already contained
   */
  public void addItem (@NonNull final ReIndexWorkItem aItem) throws IllegalStateException
  {
    ValueEnforcer.notNull (aItem, "Item");
    m_aRWLock.writeLocked ( () -> {
      internalCreateItem (aItem);
    });
    LOGGER.info ("Added " + aItem.getLogText () + " to re-try list for retry #" + (aItem.getRetryCount () + 1));
  }

  public void incRetryCountAndAddItem (@NonNull final IReIndexWorkItem aItem)
  {
    ValueEnforcer.notNull (aItem, "Item");

    // Item is not in the list anymore, therefore we need to cast it :(
    final ReIndexWorkItem aRealItem = (ReIndexWorkItem) aItem;
    m_aRWLock.writeLocked ( () -> aRealItem.incRetryCount ());
    addItem (aRealItem);
  }

  @Nullable
  public IReIndexWorkItem getAndRemoveEntry (@NonNull final Predicate <? super IReIndexWorkItem> aPred)
  {
    final IReIndexWorkItem aWorkItem = findFirst (aPred);
    if (aWorkItem == null)
      return null;

    return m_aRWLock.writeLockedGet ( () -> internalDeleteItem (aWorkItem.getID ()));
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <IReIndexWorkItem> getAndRemoveAllEntries (@NonNull final Predicate <? super IReIndexWorkItem> aFilter)
  {
    ValueEnforcer.notNull (aFilter, "Filter");
    final ICommonsList <IReIndexWorkItem> aCopyOfAll = getAll ();
    final ICommonsList <IReIndexWorkItem> ret = new CommonsArrayList <> ();
    m_aRWLock.writeLocked ( () -> {
      // Operate on a copy for removal!
      for (final IReIndexWorkItem aWorkItem : aCopyOfAll)
        if (aFilter.test (aWorkItem))
        {
          ret.add (aWorkItem);
          internalDeleteItem (aWorkItem.getID ());
        }
    });
    return ret;
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <IReIndexWorkItem> getAllItems ()
  {
    return getAll ();
  }

  @Nonnegative
  public int getItemCount ()
  {
    return size ();
  }

  @Nullable
  public IReIndexWorkItem getItemOfID (@Nullable final String sID)
  {
    return getOfID (sID);
  }
}
