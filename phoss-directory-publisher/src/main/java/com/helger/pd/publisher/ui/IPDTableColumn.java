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

import java.util.Comparator;
import java.util.Locale;
import java.util.function.Function;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.base.compare.ESortOrder;
import com.helger.base.id.IHasID;
import com.helger.base.string.StringHelper;

/**
 * Describes a single logical column of a domain object that can be sorted and/or searched by. It
 * ties the column shown in the UI to the way the underlying objects are sorted and searched, so
 * that these cannot drift apart. It is implemented as an enum per domain object.<br>
 * The IDs of these columns are the field names used in a
 * {@link com.helger.collection.paging.SortField}, and they are the only field names a client may
 * refer to - see {@link PDTableColumnHelper}.
 *
 * @author Philip Helger
 * @param <DATATYPE>
 *        The domain object type this column belongs to
 * @since 0.17.3
 */
// TODO replace with the ph-oton version com.helger.photon.core.paging.ITableColumn (since ph-oton
// 10.6.0)
public interface IPDTableColumn <DATATYPE> extends IHasID <String>
{
  /**
   * @return The provider of the text the global search is performed on. May be <code>null</code>,
   *         in which case the global search does not cover this column.
   */
  @Nullable
  Function <DATATYPE, String> getSearchValueProvider ();

  /**
   * @return <code>true</code> if the global search covers this column, <code>false</code>
   *         otherwise.
   * @see #getSearchValueProvider()
   */
  default boolean isSearchable ()
  {
    return getSearchValueProvider () != null;
  }

  /**
   * @return The comparator that sorts by this column, ascending. May be <code>null</code>, in which
   *         case the data may not be sorted by this column. A sort request referring to a
   *         non-sortable column is silently ignored.
   */
  @Nullable
  Comparator <DATATYPE> getComparator ();

  /**
   * @return <code>true</code> if the data may be sorted by this column, <code>false</code>
   *         otherwise.
   * @see #getComparator()
   */
  default boolean isSortable ()
  {
    return getComparator () != null;
  }

  /**
   * The sort order this column contributes to the <b>default order</b> - the order that is used if
   * a client requests no order at all, or only unknown respectively non-sortable ones. All columns
   * with a non-<code>null</code> value form the default order together, in their declaration
   * order.<br>
   * A default order is mandatory for paging: without a deterministic order a query returns the rows
   * of a page arbitrarily, so that consecutive pages may overlap or lose rows. Therefore at least
   * one column of a domain object must declare one, and every such column must be sortable.
   *
   * @return The sort order to be used in the default order, or <code>null</code> if this column is
   *         not part of it.
   */
  @Nullable
  ESortOrder getDefaultSortOrder ();

  /**
   * @return <code>true</code> if this column is part of the default order.
   * @see #getDefaultSortOrder()
   */
  default boolean isDefaultSortColumn ()
  {
    return getDefaultSortOrder () != null;
  }

  /**
   * Get the searchable value of this column for the provided domain object.
   *
   * @param aObj
   *        The domain object to get the value of. May not be <code>null</code>.
   * @return The value. May be <code>null</code>, especially if this column is not searchable.
   */
  @Nullable
  default String getSearchValue (@NonNull final DATATYPE aObj)
  {
    final Function <DATATYPE, String> aValueProvider = getSearchValueProvider ();
    return aValueProvider == null ? null : aValueProvider.apply (aObj);
  }

  /**
   * Check if the value of this column contains the provided search text, ignoring case.
   *
   * @param aObj
   *        The domain object to check. May not be <code>null</code>.
   * @param sSearchText
   *        The search text to be searched. May neither be <code>null</code> nor empty.
   * @return <code>true</code> if the value contains the search text.
   */
  default boolean matchesSearchText (@NonNull final DATATYPE aObj, @NonNull @Nonempty final String sSearchText)
  {
    final String sValue = getSearchValue (aObj);
    if (StringHelper.isEmpty (sValue))
      return false;
    return sValue.toLowerCase (Locale.ROOT).contains (sSearchText.toLowerCase (Locale.ROOT));
  }
}
