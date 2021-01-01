/**
 * Copyright (C) 2015-2021 Philip Helger (www.helger.com)
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

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.name.IHasDisplayName;
import com.helger.commons.type.ITypedObject;
import com.helger.pd.indexer.index.IIndexerWorkItem;

/**
 * This class holds a single item to be re-indexed. It is only invoked if
 * regular indexing failed.
 *
 * @author Philip Helger
 */
public interface IReIndexWorkItem extends ITypedObject <String>, IHasDisplayName, Serializable
{
  /**
   * @return The original work item. Never <code>null</code>.
   */
  @Nonnull
  IIndexerWorkItem getWorkItem ();

  /**
   * @return The maximum date and time until which the retry of this item
   *         occurs. Never <code>null</code>.
   */
  @Nonnull
  LocalDateTime getMaxRetryDT ();

  /**
   * @return <code>true</code> if this item is to be expired, because the
   *         retry-time has been exceeded, <code>false</code> otherwise.
   */
  default boolean isExpired ()
  {
    return getMaxRetryDT ().isBefore (PDTFactory.getCurrentLocalDateTime ());
  }

  /**
   * @return The number of retries performed so far. This counter does NOT
   *         include the original try! Always &ge; 0.
   */
  @Nonnegative
  int getRetryCount ();

  /**
   * @return The previous retry date time. If no retry happened so far, this
   *         will be <code>null</code>.
   */
  @Nullable
  LocalDateTime getPreviousRetryDT ();

  /**
   * @return <code>true</code> if a retry has already happened,
   *         <code>false</code> otherwise.
   */
  default boolean hasPreviousRetryDT ()
  {
    return getPreviousRetryDT () != null;
  }

  /**
   * @return The next retry date time. Never <code>null</code>.
   */
  @Nonnull
  LocalDateTime getNextRetryDT ();

  /**
   * Check if the passed date time qualifies the entry for the next retry.
   *
   * @param aDT
   *        The date time to check
   * @return <code>true</code> if the time for the next retry is here.
   */
  default boolean isRetryPossible (@Nonnull final LocalDateTime aDT)
  {
    return getNextRetryDT ().isBefore (aDT);
  }

  @Nonnull
  @Nonempty
  String getLogText ();

  @Nonnull
  default String getDisplayName ()
  {
    return getWorkItem ().getParticipantID ().getURIEncoded ();
  }
}
