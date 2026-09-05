/*
 * Copyright (C) 2015-2026 Philip Helger (www.helger.com)
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
package com.helger.pd.publisher.app.secure;

import java.util.Comparator;
import java.util.function.Function;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.base.compare.CompareHelper;
import com.helger.base.compare.ESortOrder;
import com.helger.pd.indexer.reindex.IReIndexWorkItem;
import com.helger.pd.publisher.ui.IPDTableColumn;
import com.helger.pd.publisher.ui.PDTableColumnHelper;

/**
 * The sortable and searchable columns of an {@link IReIndexWorkItem}, as shown on the "Re-Index
 * List" and the "Dead Index List" page. The date and the number columns are sortable only, because
 * the global search would have to match the localized text shown in the respective cell.
 *
 * @author Philip Helger
 * @since 0.17.3
 */
public enum EReIndexWorkItemColumn implements IPDTableColumn <IReIndexWorkItem>
{
  /** The date and time the work item was created */
  REG_DATE ("regdate",
            null,
            (x, y) -> CompareHelper.compare (x.getWorkItem ().getCreationDateTime (),
                                             y.getWorkItem ().getCreationDateTime (),
                                             true),
            ESortOrder.DESCENDING),
  /** The participant identifier to be re-indexed */
  PARTICIPANT ("participant", x -> x.getWorkItem ().getParticipantID ().getURIEncoded (), null),
  /** The action to be performed */
  ACTION ("action", x -> x.getWorkItem ().getType ().getDisplayName (), null),
  /** The number of retries performed so far */
  RETRIES ("retries", null, (x, y) -> CompareHelper.compare (x.getRetryCount (), y.getRetryCount ()), null),
  /** The date and time of the next retry - not shown on the "Dead Index List" page */
  NEXT_RETRY ("nextretry",
              null,
              (x, y) -> CompareHelper.compare (x.getNextRetryDT (), y.getNextRetryDT (), true),
              null),
  /** The date and time after which no further retry is performed */
  LAST_RETRY ("lastretry", null, (x, y) -> CompareHelper.compare (x.getMaxRetryDT (), y.getMaxRetryDT (), true), null);

  private final String m_sID;
  private final Function <IReIndexWorkItem, String> m_aSearchValueProvider;
  private final Comparator <IReIndexWorkItem> m_aComparator;
  private final ESortOrder m_eDefaultSortOrder;

  EReIndexWorkItemColumn (@NonNull @Nonempty final String sID,
                          @Nullable final Function <IReIndexWorkItem, String> aSearchValueProvider,
                          @Nullable final Comparator <IReIndexWorkItem> aComparator,
                          @Nullable final ESortOrder eDefaultSortOrder)
  {
    m_sID = sID;
    m_aSearchValueProvider = aSearchValueProvider;
    m_aComparator = aComparator;
    m_eDefaultSortOrder = eDefaultSortOrder;
  }

  /**
   * Constructor for a column that is sorted by the very same value the global search is performed
   * on.
   *
   * @param sID
   *        The ID of the column. May neither be <code>null</code> nor empty.
   * @param aSearchValueProvider
   *        The provider of the searchable and sortable value. May not be <code>null</code>.
   * @param eDefaultSortOrder
   *        The sort order this column contributes to the default order. May be <code>null</code>.
   */
  EReIndexWorkItemColumn (@NonNull @Nonempty final String sID,
                          @NonNull final Function <IReIndexWorkItem, String> aSearchValueProvider,
                          @Nullable final ESortOrder eDefaultSortOrder)
  {
    this (sID, aSearchValueProvider, PDTableColumnHelper.createComparator (aSearchValueProvider), eDefaultSortOrder);
  }

  @NonNull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @Nullable
  public Function <IReIndexWorkItem, String> getSearchValueProvider ()
  {
    return m_aSearchValueProvider;
  }

  @Nullable
  public Comparator <IReIndexWorkItem> getComparator ()
  {
    return m_aComparator;
  }

  @Nullable
  public ESortOrder getDefaultSortOrder ()
  {
    return m_eDefaultSortOrder;
  }
}
