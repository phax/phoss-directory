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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.base.compare.ESortOrder;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.paging.PagingSpec;
import com.helger.collection.paging.SortField;
import com.helger.pd.publisher.ui.PDTableColumnHelper;
import com.helger.peppolid.factory.PeppolIdentifierFactory;

/**
 * Test class for class {@link EContainedParticipantColumn}.
 *
 * @author Philip Helger
 */
public final class EContainedParticipantColumnTest
{
  private static final EContainedParticipantColumn [] COLUMNS = EContainedParticipantColumn.values ();

  @NonNull
  private static ContainedParticipant _create (@NonNull final String sValue, final int nEntityCount)
  {
    return new ContainedParticipant (PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme (sValue),
                                     nEntityCount);
  }

  @NonNull
  private static ICommonsList <ContainedParticipant> _list ()
  {
    return new CommonsArrayList <> (_create ("9915:bbb", 3), _create ("9915:aaa", 1), _create ("9915:ccc", 2));
  }

  @NonNull
  private static ICommonsList <String> _getAllValues (@NonNull final ICommonsList <ContainedParticipant> aList)
  {
    return aList.getAllMapped (x -> x.getParticipantID ().getValue ());
  }

  @Test
  public void testColumnProperties ()
  {
    // The participant ID is the only column the global search can be performed on
    assertTrue (EContainedParticipantColumn.PARTICIPANT.isSearchable ());
    assertTrue (EContainedParticipantColumn.PARTICIPANT.isSortable ());
    assertEquals (ESortOrder.ASCENDING, EContainedParticipantColumn.PARTICIPANT.getDefaultSortOrder ());

    // The entity count is sortable only
    assertFalse (EContainedParticipantColumn.ENTITIES.isSearchable ());
    assertTrue (EContainedParticipantColumn.ENTITIES.isSortable ());
    assertFalse (EContainedParticipantColumn.ENTITIES.isDefaultSortColumn ());
  }

  @Test
  public void testDefaultOrder ()
  {
    // No sort field at all - the default order of the columns is used
    final ICommonsList <ContainedParticipant> aPage = PDTableColumnHelper.getPage (COLUMNS,
                                                                                   _list (),
                                                                                   PagingSpec.createForPage (0, 10),
                                                                                   null);
    assertEquals (new CommonsArrayList <> ("9915:aaa", "9915:bbb", "9915:ccc"), _getAllValues (aPage));
  }

  @Test
  public void testSortByEntityCount ()
  {
    final ICommonsList <ContainedParticipant> aPage = PDTableColumnHelper.getPage (COLUMNS,
                                                                                   _list (),
                                                                                   PagingSpec.createForPage (0,
                                                                                                             10,
                                                                                                             SortField.descending (EContainedParticipantColumn.ENTITIES.getID ())),
                                                                                   null);
    assertEquals (new CommonsArrayList <> ("9915:bbb", "9915:ccc", "9915:aaa"), _getAllValues (aPage));
  }

  @Test
  public void testSearch ()
  {
    // The participant ID is searched, the entity count is not
    assertEquals (1, PDTableColumnHelper.getCount (COLUMNS, _list (), new String [] { "aaa" }));
    assertEquals (3, PDTableColumnHelper.getCount (COLUMNS, _list (), new String [] { "9915" }));
    assertEquals (0, PDTableColumnHelper.getCount (COLUMNS, _list (), new String [] { "9999" }));

    final ICommonsList <ContainedParticipant> aPage = PDTableColumnHelper.getPage (COLUMNS,
                                                                                   _list (),
                                                                                   PagingSpec.createForPage (0, 10),
                                                                                   new String [] { "ccc" });
    assertEquals (new CommonsArrayList <> ("9915:ccc"), _getAllValues (aPage));
  }
}
