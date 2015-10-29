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
package com.helger.pyp.indexer.mgr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ELockType;
import com.helger.commons.annotation.MustBeLocked;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.microdom.IMicroDocument;
import com.helger.commons.microdom.IMicroElement;
import com.helger.commons.microdom.MicroDocument;
import com.helger.commons.microdom.convert.MicroTypeConverter;
import com.helger.commons.state.EChange;
import com.helger.commons.string.ToStringGenerator;
import com.helger.photon.basic.app.dao.impl.AbstractWALDAO;
import com.helger.photon.basic.app.dao.impl.DAOException;
import com.helger.photon.basic.app.dao.impl.EDAOActionType;
import com.helger.pyp.indexer.domain.ReIndexWorkItem;

/**
 * This is the global re-index work queue.
 *
 * @author Philip Helger
 */
@ThreadSafe
final class ReIndexWorkItemList extends AbstractWALDAO <ReIndexWorkItem>
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (ReIndexWorkItemList.class);
  private static final String ELEMENT_ROOT = "root";
  private static final String ELEMENT_ITEM = "item";

  private final Map <String, ReIndexWorkItem> m_aMap = new HashMap <> ();

  public ReIndexWorkItemList (@Nullable final String sFilename) throws DAOException
  {
    super (ReIndexWorkItem.class, sFilename);
    initialRead ();
  }

  @Override
  protected void onRecoveryCreate (@Nonnull final ReIndexWorkItem aElement)
  {
    m_aMap.put (aElement.getID (), aElement);
  }

  @Override
  protected void onRecoveryUpdate (@Nonnull final ReIndexWorkItem aElement)
  {
    m_aMap.put (aElement.getID (), aElement);
  }

  @Override
  protected void onRecoveryDelete (@Nonnull final ReIndexWorkItem aElement)
  {
    m_aMap.remove (aElement.getID ());
  }

  @Override
  @Nonnull
  protected EChange onRead (@Nonnull final IMicroDocument aDoc)
  {
    for (final IMicroElement aItem : aDoc.getDocumentElement ().getAllChildElements (ELEMENT_ITEM))
      _addItem (MicroTypeConverter.convertToNative (aItem, ReIndexWorkItem.class));
    return EChange.UNCHANGED;
  }

  @Override
  @Nonnull
  @MustBeLocked (ELockType.WRITE)
  protected IMicroDocument createWriteData ()
  {
    final IMicroDocument aDoc = new MicroDocument ();
    final IMicroElement aRoot = aDoc.appendElement (ELEMENT_ROOT);
    for (final ReIndexWorkItem aWorkItem : CollectionHelper.getSortedByKey (m_aMap).values ())
      aRoot.appendChild (MicroTypeConverter.convertToMicroElement (aWorkItem, ELEMENT_ITEM));
    return aDoc;
  }

  @MustBeLocked (ELockType.WRITE)
  private void _addItem (@Nonnull final ReIndexWorkItem aItem)
  {
    ValueEnforcer.notNull (aItem, "Item");

    final String sID = aItem.getID ();
    if (m_aMap.containsKey (sID))
      throw new IllegalStateException ("Work item with ID '" + sID + "' is already contained!");
    m_aMap.put (sID, aItem);
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
    m_aRWLock.writeLock ().lock ();
    try
    {
      _addItem (aItem);
      markAsChanged (aItem, EDAOActionType.CREATE);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    s_aLogger.info ("Added " + aItem.getLogText () + " to re-try list for retry #" + (aItem.getRetryCount () + 1));
  }

  public void incRetryCountAndAddItem (@Nonnull final ReIndexWorkItem aItem)
  {
    m_aRWLock.writeLock ().lock ();
    try
    {
      aItem.incRetryCount ();
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    addItem (aItem);
  }

  @Nullable
  public ReIndexWorkItem getAndRemoveEntry (@Nonnull final Predicate <ReIndexWorkItem> aPred)
  {
    m_aRWLock.writeLock ().lock ();
    try
    {
      // Operate on a copy for removal!
      for (final ReIndexWorkItem aWorkItem : CollectionHelper.newList (m_aMap.values ()))
        if (aPred.test (aWorkItem))
        {
          m_aMap.remove (aWorkItem.getID ());
          markAsChanged (aWorkItem, EDAOActionType.DELETE);
          return aWorkItem;
        }
      return null;
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
  }

  @Nonnull
  @ReturnsMutableCopy
  public List <ReIndexWorkItem> getAndRemoveAllEntries (@Nonnull final Predicate <ReIndexWorkItem> aPred)
  {
    m_aRWLock.writeLock ().lock ();
    try
    {
      final List <ReIndexWorkItem> ret = new ArrayList <> ();
      // Operate on a copy for removal!
      for (final ReIndexWorkItem aWorkItem : CollectionHelper.newList (m_aMap.values ()))
        if (aPred.test (aWorkItem))
        {
          ret.add (aWorkItem);
          m_aMap.remove (aWorkItem.getID ());
          markAsChanged (aWorkItem, EDAOActionType.DELETE);
        }
      return ret;
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
  }

  @Nonnull
  @ReturnsMutableCopy
  public List <ReIndexWorkItem> getAllItems ()
  {
    m_aRWLock.readLock ().lock ();
    try
    {
      return CollectionHelper.newList (m_aMap.values ());
    }
    finally
    {
      m_aRWLock.readLock ().unlock ();
    }
  }

  @Override
  public String toString ()
  {
    return ToStringGenerator.getDerived (super.toString ()).append ("Map", m_aMap).toString ();
  }
}
