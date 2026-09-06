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
package com.helger.pd.publisher.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Comparator;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Test;

import com.helger.base.compare.ESortOrder;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSet;
import com.helger.collection.paging.PagingSpec;
import com.helger.collection.paging.SortField;
import com.helger.pd.publisher.app.secure.EContainedParticipantColumn;
import com.helger.pd.publisher.app.secure.EIndexerWorkItemColumn;
import com.helger.pd.publisher.app.secure.EReIndexWorkItemColumn;
import com.helger.photon.core.paging.ITableColumn;
import com.helger.photon.core.paging.SortColumn;
import com.helger.photon.core.paging.TableColumnHelper;

/**
 * Test class for class {@link TableColumnHelper}.
 *
 * @author Philip Helger
 */
public final class PDTableColumnHelperTest
{
  /**
   * A minimal column implementation on plain Strings, so that the helper can be tested without any
   * domain object scaffolding.
   *
   * @author Philip Helger
   */
  private static final class MockColumn implements ITableColumn <String>
  {
    private final String m_sID;
    private final Function <String, String> m_aSearchValueProvider;
    private final Comparator <String> m_aComparator;
    private final ESortOrder m_eDefaultSortOrder;

    MockColumn (@NonNull final String sID,
                @Nullable final Function <String, String> aSearchValueProvider,
                @Nullable final Comparator <String> aComparator,
                @Nullable final ESortOrder eDefaultSortOrder)
    {
      m_sID = sID;
      m_aSearchValueProvider = aSearchValueProvider;
      m_aComparator = aComparator;
      m_eDefaultSortOrder = eDefaultSortOrder;
    }

    public String getID ()
    {
      return m_sID;
    }

    @Nullable
    public Function <String, String> getSearchValueProvider ()
    {
      return m_aSearchValueProvider;
    }

    @Nullable
    public Comparator <String> getComparator ()
    {
      return m_aComparator;
    }

    @Nullable
    public ESortOrder getDefaultSortOrder ()
    {
      return m_eDefaultSortOrder;
    }
  }

  /** The whole String, sortable, searchable and the default order */
  private static final MockColumn COL_ALL = new MockColumn ("all",
                                                            Function.identity (),
                                                            TableColumnHelper.createComparator (Function.identity ()),
                                                            ESortOrder.ASCENDING);
  /** The length of the String - sortable but not searchable */
  private static final MockColumn COL_LENGTH = new MockColumn ("length",
                                                               null,
                                                               Comparator.comparingInt (String::length),
                                                               null);
  /** The first character of the String - searchable but not sortable */
  private static final MockColumn COL_FIRST = new MockColumn ("first", x -> x.substring (0, 1), null, null);

  private static final ITableColumn <String> [] COLUMNS = new MockColumn [] { COL_ALL, COL_LENGTH, COL_FIRST };

  @NonNull
  private static ICommonsList <String> _list ()
  {
    return new CommonsArrayList <> ("bbb", "a", "cc", "dddd");
  }

  @Test
  public void testFindColumn ()
  {
    assertSame (COL_ALL, TableColumnHelper.findColumn (COLUMNS, "all"));
    assertSame (COL_LENGTH, TableColumnHelper.findColumn (COLUMNS, "length"));
    assertNull (TableColumnHelper.findColumn (COLUMNS, "bla"));
    assertNull (TableColumnHelper.findColumn (COLUMNS, ""));
    assertNull (TableColumnHelper.findColumn (COLUMNS, null));
  }

  @Test
  public void testDefaultSortFields ()
  {
    final ICommonsList <SortField> aSortFields = TableColumnHelper.getAllDefaultSortFields (COLUMNS);
    assertEquals (1, aSortFields.size ());
    assertEquals ("all", aSortFields.getFirstOrNull ().getFieldName ());
    assertEquals (ESortOrder.ASCENDING, aSortFields.getFirstOrNull ().getSortOrder ());
  }

  @Test
  public void testSortColumnsFallbackToDefault ()
  {
    // An unknown field falls back to the default order
    ICommonsList <SortColumn <String>> aSortColumns = TableColumnHelper.getAllSortColumns (COLUMNS,
                                                                                           PagingSpec.createUnlimited (SortField.ascending ("bla")));
    assertEquals (1, aSortColumns.size ());
    assertSame (COL_ALL, aSortColumns.getFirstOrNull ().getColumn ());

    // A non-sortable field falls back to the default order as well
    aSortColumns = TableColumnHelper.getAllSortColumns (COLUMNS,
                                                        PagingSpec.createUnlimited (SortField.ascending ("first")));
    assertEquals (1, aSortColumns.size ());
    assertSame (COL_ALL, aSortColumns.getFirstOrNull ().getColumn ());

    // A known and sortable field is used
    aSortColumns = TableColumnHelper.getAllSortColumns (COLUMNS,
                                                        PagingSpec.createUnlimited (SortField.descending ("length")));
    assertEquals (1, aSortColumns.size ());
    assertSame (COL_LENGTH, aSortColumns.getFirstOrNull ().getColumn ());
    assertFalse (aSortColumns.getFirstOrNull ().isAscending ());
  }

  @Test
  public void testSearchPredicate ()
  {
    assertNull (TableColumnHelper.getSearchPredicate (COLUMNS, (String) null));
    assertNull (TableColumnHelper.getSearchPredicate (COLUMNS, new String [0]));

    // Case insensitive "contains" on any searchable column
    Predicate <String> aFilter = TableColumnHelper.getSearchPredicate (COLUMNS, new String [] { "BB" });
    assertNotNull (aFilter);
    assertTrue (aFilter.test ("bbb"));
    assertFalse (aFilter.test ("cc"));

    // All search terms must match - "b" via the whole value, "c" via nothing
    aFilter = TableColumnHelper.getSearchPredicate (COLUMNS, new String [] { "b", "c" });
    assertNotNull (aFilter);
    assertFalse (aFilter.test ("bbb"));
    assertTrue (aFilter.test ("bc"));

    // The first character is searchable, the length is not
    aFilter = TableColumnHelper.getSearchPredicate (COLUMNS, new String [] { "d" });
    assertNotNull (aFilter);
    assertTrue (aFilter.test ("dddd"));
  }

  @Test
  public void testGetPage ()
  {
    // Sorted by the default order, first page
    ICommonsList <String> aPage = TableColumnHelper.getPage (COLUMNS,
                                                             _list (),
                                                             PagingSpec.createForPage (0, 2),
                                                             (String) null);
    assertEquals (new CommonsArrayList <> ("a", "bbb"), aPage);

    // Second page
    aPage = TableColumnHelper.getPage (COLUMNS, _list (), PagingSpec.createForPage (1, 2), (String) null);
    assertEquals (new CommonsArrayList <> ("cc", "dddd"), aPage);

    // Sorted by the length, descending
    aPage = TableColumnHelper.getPage (COLUMNS,
                                       _list (),
                                       PagingSpec.createForPage (0, 2, SortField.descending ("length")),
                                       (String) null);
    assertEquals (new CommonsArrayList <> ("dddd", "bbb"), aPage);

    // Filtered and paged
    aPage = TableColumnHelper.getPage (COLUMNS, _list (), PagingSpec.createForPage (0, 10), new String [] { "c" });
    assertEquals (new CommonsArrayList <> ("cc"), aPage);

    // Beyond the last page
    aPage = TableColumnHelper.getPage (COLUMNS, _list (), PagingSpec.createForPage (10, 2), (String) null);
    assertTrue (aPage.isEmpty ());
  }

  @Test
  public void testGetCount ()
  {
    assertEquals (4, TableColumnHelper.getCount (COLUMNS, _list (), (String) null));
    assertEquals (4, TableColumnHelper.getCount (COLUMNS, _list (), new String [0]));
    assertEquals (1, TableColumnHelper.getCount (COLUMNS, _list (), new String [] { "c" }));
    assertEquals (0, TableColumnHelper.getCount (COLUMNS, _list (), new String [] { "x" }));
  }

  private static void _testRealColumns (@NonNull final ITableColumn <?> [] aColumns)
  {
    final ICommonsSet <String> aIDs = new CommonsHashSet <> ();
    for (final ITableColumn <?> aColumn : aColumns)
    {
      assertTrue ("The column ID '" + aColumn.getID () + "' is contained more than once", aIDs.add (aColumn.getID ()));
      // A column of the default order must be sortable, because paging without a deterministic
      // order returns arbitrary rows
      if (aColumn.isDefaultSortColumn ())
        assertTrue ("The column '" + aColumn.getID () + "' is not sortable", aColumn.isSortable ());
    }
    assertFalse ("No column declares a default sort order",
                 TableColumnHelper.getAllDefaultSortFields (aColumns).isEmpty ());
  }

  @Test
  public void testRealColumns ()
  {
    _testRealColumns (EIndexerWorkItemColumn.values ());
    _testRealColumns (EReIndexWorkItemColumn.values ());
    _testRealColumns (EContainedParticipantColumn.values ());
  }
}
