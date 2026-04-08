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
 * Persistent dead-letter queue (DLQ) for failed shadow events. Events in this
 * queue have been rejected by the downstream service with non-retryable errors
 * and require manual investigation.
 * <p>
 * Operators can inspect the failed-shadow-events.xml file to review failures
 * and manually move events back to the live queue (shadow-events.xml) if
 * appropriate.
 * </p>
 *
 * @author Mikael Aksamit
 */
@ThreadSafe
public final class FailedShadowEventList extends AbstractPhotonMapBasedWALDAO <IShadowEvent, ShadowEvent> implements
                                          IFailedShadowEventList
{
  private static final Logger LOGGER = LoggerFactory.getLogger (FailedShadowEventList.class);

  public FailedShadowEventList () throws DAOException
  {
    super (ShadowEvent.class, "failed-shadow-events.xml");
  }

  public void addFailedEvent (@Nonnull final ShadowEvent aEvent)
  {
    ValueEnforcer.notNull (aEvent, "Event");
    m_aRWLock.writeLocked ( () -> {
      internalCreateItem (aEvent);
    });
    LOGGER.warn ("Added shadow event to DLQ (non-retryable failure): " + aEvent.getEventID ());
  }

  public void removeFailedEvent (@Nonnull final String sEventID)
  {
    ValueEnforcer.notNull (sEventID, "EventID");
    m_aRWLock.writeLocked ( () -> {
      internalDeleteItem (sEventID);
    });
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Removed failed shadow event from DLQ: " + sEventID);
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IShadowEvent> getAllFailedEvents ()
  {
    return getAll ();
  }

  @Nonnegative
  public int getFailedEventCount ()
  {
    return size ();
  }

  @Nullable
  public IShadowEvent getFailedEventOfID (@Nullable final String sEventID)
  {
    return getOfID (sEventID);
  }
}
