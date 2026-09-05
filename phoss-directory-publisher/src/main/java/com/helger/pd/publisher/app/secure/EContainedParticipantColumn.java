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
import com.helger.pd.publisher.ui.IPDTableColumn;
import com.helger.pd.publisher.ui.PDTableColumnHelper;

/**
 * The sortable and searchable columns of a {@link ContainedParticipant}, as shown on the
 * "Participant list" page. The number column is sortable only, because the global search would have
 * to match the localized text shown in the respective cell.
 *
 * @author Philip Helger
 * @since 0.17.3
 */
public enum EContainedParticipantColumn implements IPDTableColumn <ContainedParticipant>
{
  /** The participant identifier */
  PARTICIPANT ("participant", x -> x.getParticipantID ().getURIEncoded (), ESortOrder.ASCENDING),
  /** The number of business entities stored for the participant */
  ENTITIES ("entities", null, (x, y) -> CompareHelper.compare (x.getEntityCount (), y.getEntityCount ()), null);

  private final String m_sID;
  private final Function <ContainedParticipant, String> m_aSearchValueProvider;
  private final Comparator <ContainedParticipant> m_aComparator;
  private final ESortOrder m_eDefaultSortOrder;

  EContainedParticipantColumn (@NonNull @Nonempty final String sID,
                               @Nullable final Function <ContainedParticipant, String> aSearchValueProvider,
                               @Nullable final Comparator <ContainedParticipant> aComparator,
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
  EContainedParticipantColumn (@NonNull @Nonempty final String sID,
                               @NonNull final Function <ContainedParticipant, String> aSearchValueProvider,
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
  public Function <ContainedParticipant, String> getSearchValueProvider ()
  {
    return m_aSearchValueProvider;
  }

  @Nullable
  public Comparator <ContainedParticipant> getComparator ()
  {
    return m_aComparator;
  }

  @Nullable
  public ESortOrder getDefaultSortOrder ()
  {
    return m_eDefaultSortOrder;
  }
}
