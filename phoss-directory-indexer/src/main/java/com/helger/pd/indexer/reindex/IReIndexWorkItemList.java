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

import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.state.EChange;
import com.helger.collection.commons.ICommonsList;

import jakarta.annotation.Nullable;

/**
 * Base interface for {@link ReIndexWorkItem} objects.
 *
 * @author Philip Helger
 */
public interface IReIndexWorkItemList
{
  /**
   * @return A list of all re-index items currently in the list. Never
   *         <code>null</code> but maybe empty.
   */
  @NonNull
  @ReturnsMutableCopy
  ICommonsList <? extends IReIndexWorkItem> getAllItems ();

  /**
   * @return The number of contained items. Always &ge; 0.
   */
  @Nonnegative
  int getItemCount ();

  /**
   * Get the item with the specified ID.
   *
   * @param sID
   *        The ID to search. May be <code>null</code>.
   * @return <code>null</code> if no such item exists.
   */
  @Nullable
  IReIndexWorkItem getItemOfID (@Nullable String sID);

  /**
   * Find and remove the first work item matching the provided predicate.
   *
   * @param aFilter
   *        The predicate to use. May not be <code>null</code>.
   * @return <code>null</code> if no such entry exists.
   */
  @Nullable
  IReIndexWorkItem getAndRemoveEntry (@NonNull Predicate <? super IReIndexWorkItem> aFilter);

  @NonNull
  default EChange deleteItem (@Nullable final String sID)
  {
    return EChange.valueOf (sID != null && getAndRemoveEntry (x -> x.getID ().equals (sID)) != null);
  }

  /**
   * Remove all work items matching the provided predicate.
   *
   * @param aFilter
   *        The predicate to use. May not be <code>null</code>.
   * @return <code>null</code> if no such entry exists.
   */
  @NonNull
  @ReturnsMutableCopy
  ICommonsList <IReIndexWorkItem> getAndRemoveAllEntries (@NonNull Predicate <? super IReIndexWorkItem> aFilter);

  @NonNull
  default EChange deleteAllItems ()
  {
    return EChange.valueOf (getAndRemoveAllEntries (x -> true).isNotEmpty ());
  }
}
