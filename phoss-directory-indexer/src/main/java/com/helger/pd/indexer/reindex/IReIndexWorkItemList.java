/**
 * Copyright (C) 2015-2020 Philip Helger (www.helger.com)
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

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.EChange;

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
  @Nonnull
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
   * @param aPred
   *        The predicate to use. May not be <code>null</code>.
   * @return <code>null</code> if no such entry exists.
   */
  @Nullable
  IReIndexWorkItem getAndRemoveEntry (@Nonnull Predicate <? super IReIndexWorkItem> aPred);

  @Nonnull
  default EChange deleteItem (@Nullable final String sID)
  {
    return EChange.valueOf (sID != null && getAndRemoveEntry (x -> x.getID ().equals (sID)) != null);
  }
}
