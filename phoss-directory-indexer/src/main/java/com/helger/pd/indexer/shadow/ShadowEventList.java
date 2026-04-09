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
package com.helger.pd.indexer.shadow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.collection.commons.ICommonsList;
import com.helger.dao.DAOException;
import com.helger.photon.io.dao.AbstractPhotonMapBasedWALDAO;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Persistent list of shadow events using Write-Ahead Log DAO pattern. This is
 * the live queue for events pending dispatch to the downstream service.
 * <p>
 * <strong>Important:</strong> This implementation assumes a single application
 * instance per data directory. If multiple instances share the same data
 * directory, file corruption may occur.
 * </p>
 *
 * @author Mikael Aksamit
 */
@ThreadSafe
public final class ShadowEventList extends AbstractPhotonMapBasedWALDAO <IShadowEvent, ShadowEvent> implements
                                   IShadowEventList
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ShadowEventList.class);

  public ShadowEventList () throws DAOException
  {
    super (ShadowEvent.class, "shadow-events.xml");
  }

  public void addEvent (@Nonnull final ShadowEvent aEvent)
  {
    ValueEnforcer.notNull (aEvent, "Event");
    m_aRWLock.writeLocked ( () -> {
      internalCreateItem (aEvent);
    });
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Added shadow event to queue: " + aEvent.getEventID ());
  }

  public void removeEvent (@Nonnull final String sEventID)
  {
    ValueEnforcer.notNull (sEventID, "EventID");
    m_aRWLock.writeLocked ( () -> {
      internalDeleteItem (sEventID);
    });
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Removed shadow event from queue: " + sEventID);
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IShadowEvent> getAllEvents ()
  {
    return getAll ();
  }

  @Nonnegative
  public int getEventCount ()
  {
    return size ();
  }

  @Nullable
  public IShadowEvent getEventOfID (@Nullable final String sEventID)
  {
    return getOfID (sEventID);
  }
}
