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

import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.collection.commons.ICommonsList;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Interface for managing failed shadow events in a dead-letter queue (DLQ).
 * Events in this queue have been rejected by the downstream service with
 * non-retryable errors (typically 4xx HTTP status codes) and require manual
 * investigation.
 *
 * @author Mikael Aksamit
 */
public interface IFailedShadowEventList
{
  /**
   * Add a failed shadow event to the DLQ.
   *
   * @param aEvent
   *        The event to add. May not be <code>null</code>.
   */
  void addFailedEvent (@Nonnull ShadowEvent aEvent);

  /**
   * Remove a failed shadow event from the DLQ by its ID.
   *
   * @param sEventID
   *        The event ID. May not be <code>null</code>.
   */
  void removeFailedEvent (@Nonnull String sEventID);

  /**
   * Get all failed shadow events.
   *
   * @return A copy of all failed events. Never <code>null</code>.
   */
  @Nonnull
  @ReturnsMutableCopy
  ICommonsList <IShadowEvent> getAllFailedEvents ();

  /**
   * Get the number of failed events in the DLQ.
   *
   * @return The failed event count. Always &ge; 0.
   */
  @Nonnegative
  int getFailedEventCount ();

  /**
   * Get a specific failed event by its ID.
   *
   * @param sEventID
   *        The event ID. May be <code>null</code>.
   * @return The event or <code>null</code> if not found.
   */
  @Nullable
  IShadowEvent getFailedEventOfID (@Nullable String sEventID);
}
