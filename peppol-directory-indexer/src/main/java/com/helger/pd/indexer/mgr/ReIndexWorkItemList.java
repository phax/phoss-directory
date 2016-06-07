/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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

import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.pd.indexer.domain.IReIndexWorkItem;
import com.helger.pd.indexer.domain.ReIndexWorkItem;
import com.helger.photon.basic.app.dao.impl.AbstractMapBasedWALDAO;
import com.helger.photon.basic.app.dao.impl.DAOException;

/**
 * This is the global re-index work queue. It is solely used in the
 * {@link PDIndexerManager}.
 *
 * @author Philip Helger
 */
@ThreadSafe
final class ReIndexWorkItemList extends AbstractMapBasedWALDAO <IReIndexWorkItem, ReIndexWorkItem>
                                implements IReIndexWorkItemList
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (ReIndexWorkItemList.class);

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
  public void addItem (@Nonnull final ReIndexWorkItem aItem) throws IllegalStateException
  {
    ValueEnforcer.notNull (aItem, "Item");
    m_aRWLock.writeLocked ( () -> {
      internalCreateItem (aItem);
    });
    s_aLogger.info ("Added " + aItem.getLogText () + " to re-try list for retry #" + (aItem.getRetryCount () + 1));
  }

  public void incRetryCountAndAddItem (@Nonnull final IReIndexWorkItem aItem)
  {
    ValueEnforcer.notNull (aItem, "Item");

    final ReIndexWorkItem aRealItem = getOfID (aItem.getID ());
    if (aRealItem != null)
    {
      m_aRWLock.writeLocked ( () -> aRealItem.incRetryCount ());
      addItem (aRealItem);
    }
  }

  @Nullable
  public IReIndexWorkItem getAndRemoveEntry (@Nonnull final Predicate <? super IReIndexWorkItem> aPred)
  {
    final IReIndexWorkItem aWorkItem = findFirst (aPred);
    if (aWorkItem == null)
      return null;

    m_aRWLock.writeLocked ( () -> {
      internalDeleteItem (aWorkItem.getID ());
    });
    return aWorkItem;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IReIndexWorkItem> getAndRemoveAllEntries (@Nonnull final Predicate <? super IReIndexWorkItem> aPred)
  {
    final ICommonsList <? extends IReIndexWorkItem> aCopyOfAll = getAll ();
    final ICommonsList <IReIndexWorkItem> ret = new CommonsArrayList<> ();
    m_aRWLock.writeLocked ( () -> {
      // Operate on a copy for removal!
      for (final IReIndexWorkItem aWorkItem : aCopyOfAll)
        if (aPred.test (aWorkItem))
        {
          ret.add (aWorkItem);
          internalDeleteItem (aWorkItem.getID ());
        }
    });
    return ret;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <? extends IReIndexWorkItem> getAllItems ()
  {
    return getAll ();
  }

  @Nullable
  public IReIndexWorkItem getItemOfID (@Nullable final String sID)
  {
    return getOfID (sID);
  }
}
