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
package com.helger.pd.indexer.storage;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.CheckForSigned;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.collection.commons.ICommonsList;
import com.helger.pd.indexer.searchindex.query.IPDIndexQuery;

/**
 * The result of a single search index query, as returned by
 * {@link PDStorageManager#search(IPDIndexQuery, int)}. It contains the matching business entities,
 * limited by the maximum result count of the search, as well as the total number of matching
 * business entities, that is independent of that limit.
 *
 * @param allEntities
 *        All matching business entities, at most as many as the maximum result count of the search
 *        allows. May not be <code>null</code> but maybe empty. The list is newly created for each
 *        search and may therefore be modified by the caller.
 * @param totalHitCount
 *        The total number of business entities matching the query, independent of the maximum
 *        result count of the search. It is &lt; 0 if the search failed.
 * @author Philip Helger
 * @since 0.18.0
 */
public record PDSearchResult (@NonNull ICommonsList <PDStoredBusinessEntity> allEntities,
                              @CheckForSigned int totalHitCount)
{
  public PDSearchResult
  {
    ValueEnforcer.notNull (allEntities, "AllEntities");
  }
}
