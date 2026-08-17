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
package com.helger.pd.indexer.searchindex.query;

import jakarta.annotation.Nullable;

/**
 * Base interface for all search index queries. All queries are independent of the underlying search
 * engine - it is up to the respective implementation of
 * {@link com.helger.pd.indexer.searchindex.IPDIndex} to translate them into the native query
 * representation.<br>
 * The set of possible queries is closed - these are the implementations:
 * <ul>
 * <li>{@link PDIndexQueryMatchAll} - match all documents</li>
 * <li>{@link PDIndexQueryTerm} - exact match of a single field value</li>
 * <li>{@link PDIndexQueryPrefix} - "starts with" match of a single field value</li>
 * <li>{@link PDIndexQueryContains} - "contains" match of a single field value</li>
 * <li>{@link PDIndexQueryBool} - boolean combination of other queries</li>
 * </ul>
 * Every implementation must provide a stable <code>toString</code> representation, because it is
 * used for logging and as the key of the query runtime statistics.
 *
 * @author Philip Helger
 */
public interface IPDIndexQuery
{
  /**
   * Get the cached search engine specific representation of this query. This method is reserved for
   * the implementations of {@link com.helger.pd.indexer.searchindex.IPDIndex} - they are the only
   * ones that know how to interpret the returned object.
   *
   * @return <code>null</code> if this query was not yet translated.
   * @see #setNativeQuery(Object)
   */
  @Nullable
  Object getNativeQuery ();

  /**
   * Remember the search engine specific representation of this query, so that repeated executions
   * of the same query object don't need to translate it again. This method is reserved for the
   * implementations of {@link com.helger.pd.indexer.searchindex.IPDIndex}. Implementations that use
   * this cache must check the type of the cached object, because in theory more than one search
   * engine can be active at the same time.
   *
   * @param aNativeQuery
   *        The translated query to be cached. May be <code>null</code> to clear the cache.
   * @see #getNativeQuery()
   */
  void setNativeQuery (@Nullable Object aNativeQuery);
}
