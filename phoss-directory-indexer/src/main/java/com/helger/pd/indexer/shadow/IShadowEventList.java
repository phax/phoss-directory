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
 * Interface for managing shadow events in a persistent queue.
 *
 * @author Mikael Aksamit
 */
public interface IShadowEventList
{
  /**
   * Add a shadow event to the queue.
   *
   * @param aEvent
   *        The event to add. May not be <code>null</code>.
   */
  void addEvent (@Nonnull ShadowEvent aEvent);

  /**
   * Remove a shadow event from the queue by its ID.
   *
   * @param sEventID
   *        The event ID. May not be <code>null</code>.
   */
  void removeEvent (@Nonnull String sEventID);

  /**
   * Get all pending shadow events.
   *
   * @return A copy of all pending events. Never <code>null</code>.
   */
  @Nonnull
  @ReturnsMutableCopy
  ICommonsList <IShadowEvent> getAllEvents ();

  /**
   * Get the number of pending events in the queue.
   *
   * @return The event count. Always &ge; 0.
   */
  @Nonnegative
  int getEventCount ();

  /**
   * Get a specific event by its ID.
   *
   * @param sEventID
   *        The event ID. May be <code>null</code>.
   * @return The event or <code>null</code> if not found.
   */
  @Nullable
  IShadowEvent getEventOfID (@Nullable String sEventID);
}
