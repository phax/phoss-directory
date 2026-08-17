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

import com.helger.annotation.concurrent.ThreadSafe;

import jakarta.annotation.Nullable;

/**
 * Abstract base class for all queries. It only provides the cache for the search engine specific
 * representation of the query. The cached value is a derived value only - it is not part of the
 * identity of a query and therefore not considered in <code>equals</code> and
 * <code>hashCode</code>.
 *
 * @author Philip Helger
 * @since 0.16.0
 */
@ThreadSafe
public abstract class AbstractPDIndexQuery implements IPDIndexQuery
{
  // Only a derived value - concurrently creating it twice is harmless
  private volatile Object m_aNativeQuery;

  @Nullable
  public final Object getNativeQuery ()
  {
    return m_aNativeQuery;
  }

  public final void setNativeQuery (@Nullable final Object aNativeQuery)
  {
    m_aNativeQuery = aNativeQuery;
  }
}
